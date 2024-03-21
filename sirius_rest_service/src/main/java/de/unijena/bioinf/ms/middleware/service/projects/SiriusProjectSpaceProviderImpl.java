/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schiller University.
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.ms.middleware.service.projects;

import de.unijena.bioinf.ChemistryBase.utils.FileUtils;
import de.unijena.bioinf.ms.middleware.SiriusMiddlewareApplication;
import de.unijena.bioinf.ms.middleware.model.events.ProjectChangeEvent;
import de.unijena.bioinf.ms.middleware.model.events.ServerEventImpl;
import de.unijena.bioinf.ms.middleware.model.events.ServerEvents;
import de.unijena.bioinf.ms.middleware.model.projects.ProjectInfo;
import de.unijena.bioinf.ms.middleware.service.events.EventService;
import de.unijena.bioinf.projectspace.*;
import de.unijena.bioinf.rest.NetUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;
import java.util.stream.Collectors;

import static de.unijena.bioinf.ms.middleware.model.events.ProjectChangeEvent.Type.*;
import static de.unijena.bioinf.projectspace.ProjectSpaceIO.*;

public class SiriusProjectSpaceProviderImpl implements ProjectsProvider<SiriusProjectSpaceImpl> {
    //todo extract methods to abstract implementation where possible
    private final ProjectSpaceManagerFactory<SiriusProjectSpaceManager> projectSpaceManagerFactory;

    private final HashMap<String, SiriusProjectSpaceManager> projectSpaces = new HashMap<>();

    protected final ReadWriteLock projectSpaceLock = new ReentrantReadWriteLock();

    private final EventService<?> eventService;

    public SiriusProjectSpaceProviderImpl(ProjectSpaceManagerFactory<? extends ProjectSpaceManager> projectSpaceManagerFactory, EventService<?> eventService) {
        this.projectSpaceManagerFactory = (ProjectSpaceManagerFactory<SiriusProjectSpaceManager>) projectSpaceManagerFactory;
        this.eventService = eventService;
    }


    public List<ProjectInfo> listAllProjectSpaces() {
        projectSpaceLock.readLock().lock();
        try {
            return projectSpaces.entrySet().stream().map(x -> ProjectInfo.of(x.getKey(), x.getValue().getLocation())).collect(Collectors.toList());
        } finally {
            projectSpaceLock.readLock().unlock();
        }
    }

    public Optional<SiriusProjectSpaceImpl> getProject(String name) {
        return getProjectSpace(name).map(ps -> new SiriusProjectSpaceImpl(name, ps));
    }

    protected Optional<SiriusProjectSpaceManager> getProjectSpace(String name) {
        projectSpaceLock.readLock().lock();
        try {
            return Optional.ofNullable(projectSpaces.get(name));
        } finally {
            projectSpaceLock.readLock().unlock();
        }
    }

    @Override
    public Optional<ProjectInfo> getProjectInfo(@NotNull String name, @NotNull EnumSet<ProjectInfo.OptField> optFields) {
        return getProjectSpace(name).map(x -> createProjectInfo(name, x, optFields));
    }

    /**
     * either use the suggested name, or add some suffix to the name such that it becomes unique during the call
     * of the provided function
     */
    public <S> S ensureUniqueName(String suggestion, Function<String, S> useUniqueName) {
        final Lock lock = projectSpaceLock.writeLock();
        lock.lock();
        try {
            if (!projectSpaces.containsKey(suggestion)) {
                return useUniqueName.apply(suggestion);
            } else {
                int index = 2;
                while (projectSpaces.containsKey(suggestion + "_" + index)) {
                    ++index;
                }
                return useUniqueName.apply(suggestion + "_" + index);
            }
        } finally {
            lock.unlock();
        }
    }

    private ProjectInfo createProjectInfo(String projectId, SiriusProjectSpaceManager psm,
                                          @NotNull EnumSet<ProjectInfo.OptField> optFields) {
        SiriusProjectSpace rawProject = psm.getProjectSpaceImpl();
        ProjectInfo.ProjectInfoBuilder b = ProjectInfo.builder()
                .projectId(projectId).location(rawProject.getLocation().toString());
        if (optFields.contains(ProjectInfo.OptField.sizeInformation))
            b.numOfBytes(FileUtils.getFolderSizeOrThrow(rawProject.getLocation()))
                    .numOfFeatures(rawProject.size());
        if (optFields.contains(ProjectInfo.OptField.compatibilityInfo))
            b.compatible(InstanceImporter.checkDataCompatibility(rawProject, NetUtils.checkThreadInterrupt(Thread.currentThread())) == null);

        return b.build();
    }

    @Override
    public ProjectInfo openProjectSpace(@NotNull String projectId, @Nullable String pathToProject, @NotNull EnumSet<ProjectInfo.OptField> optFields) throws IOException {
        projectId = ensureUniqueProjectId(validateId(projectId));
        final Lock lock = projectSpaceLock.writeLock();
        lock.lock();
        try {
            if (projectSpaces.containsKey(projectId)) {
                throw new ResponseStatusException(HttpStatus.SEE_OTHER, "A project with id '" + projectId + "' is already opened.");
            }

            Path location = pathToProject != null && !pathToProject.isBlank() ? Path.of(pathToProject) : defaultProjectDir().resolve(projectId);
            if (!isExistingProjectspaceDirectory(location) && !isZipProjectSpace(location)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "'" + projectId + "' is no valid SIRIUS project space.");
            }

            return createOrOpen(projectId, location, optFields);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public ProjectInfo createProjectSpace(@NotNull String projectIdSuggestion, @Nullable String path, @NotNull EnumSet<ProjectInfo.OptField> optFields, boolean failIfExists) {
        return ensureUniqueName(validateId(projectIdSuggestion), (projectId) -> {
            try {
                Path location = path != null && !path.isBlank() ? Path.of(path) : defaultProjectDir().resolve(projectId);

                if (!failIfExists)
                    if (Files.exists(location) && !(Files.isDirectory(location) && FileUtils.listAndClose(location, s -> s.findAny().isEmpty())))
                        throw new ResponseStatusException(HttpStatus.CONFLICT, "Location '" + location.toAbsolutePath() +
                                "' already exists and is not an empty directory. Cannot create new project space here.");

                return createOrOpen(projectId, location, optFields);
            } catch (IOException e) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error when accessing file system to create project.", e);
            }
        });
    }

    private ProjectInfo createOrOpen(String projectId, Path location, @NotNull EnumSet<ProjectInfo.OptField> optFields) throws IOException {
        SiriusProjectSpaceManager psm = projectSpaceManagerFactory.createOrOpen(location);

        registerEventListeners(projectId, psm);
        projectSpaces.put(projectId, psm);
        eventService.sendEvent(ServerEvents.newProjectEvent(projectId, PROJECT_OPENED));
        return createProjectInfo(projectId, psm, optFields);
    }

    @Override
    public boolean containsProject(@NotNull String projectId) {
        return projectSpaces.containsKey(projectId);
    }

    public void closeProjectSpace(String projectId) throws IOException {
        projectSpaceLock.writeLock().lock();
        try {
            final SiriusProjectSpaceManager space = projectSpaces.get(projectId);
            if (space == null) {
                throw new ResponseStatusException(HttpStatus.NO_CONTENT, "Project space with name '" + projectId + "' not found!");
            }
            space.close();
            projectSpaces.remove(projectId);
        } finally {
            projectSpaceLock.writeLock().unlock();
        }
    }

    @Override
    public ProjectInfo copyProjectSpace(@NotNull String sourceProjectId, @NotNull String copyPathToProject, @Nullable String copyId, @NotNull EnumSet<ProjectInfo.OptField> optFields) throws IOException {
        ProjectInfo old = getProjectInfoOrThrow(sourceProjectId, optFields);
        Path copyPath = Path.of(copyPathToProject).normalize();
        if (Path.of(old.getLocation()).normalize().equals(copyPath))
            return old;

        SiriusProjectSpaceImpl ps = getProjectOrThrow(sourceProjectId);
        copyProject(ps.getProjectSpaceManager(), copyPath);

        //open new project as well
        if (copyId != null)
            return openProjectSpace(copyId, copyPathToProject, optFields);

        return old;
    }

    protected void copyProject(SiriusProjectSpaceManager psm, Path copyPath) throws IOException {
        ProjectSpaceIO.copyProject(psm.getProjectSpaceImpl(), copyPath, false);
    }

    @Override
    public void closeAll() {
        projectSpaceLock.writeLock().lock();
        try {
            LoggerFactory.getLogger(SiriusMiddlewareApplication.class).info("Closing Projects...'");
            projectSpaces.values().forEach(ps -> {
                try {
                    ps.close();
                    LoggerFactory.getLogger(SiriusMiddlewareApplication.class).info("Project: '" + ps.getLocation() + "' successfully closed.");
                } catch (IOException e) {
                    LoggerFactory.getLogger(getClass()).error("Error when closing Project-Space '" + ps.getLocation() + "'. Data might be corrupted.");
                }
            });
            projectSpaces.clear();
        } finally {
            projectSpaceLock.writeLock().unlock();
        }
    }

    /**
     * registers listeners that will transform project space events into server events to be sent via rest api*
     */
    private void registerEventListeners(@NotNull String id, @NotNull SiriusProjectSpaceManager psm) {
        SiriusProjectSpace project = psm.getProjectSpaceImpl();
        project.addProjectSpaceListener(projectSpaceEvent -> {
            switch (projectSpaceEvent) {
                case OPENED -> eventService.sendEvent(ServerEvents.newProjectEvent(id, PROJECT_OPENED));
                case CLOSED -> eventService.sendEvent(ServerEvents.newProjectEvent(id, PROJECT_CLOSED));
                case LOCATION_CHANGED -> eventService.sendEvent(ServerEvents.newProjectEvent(id, PROJECT_MOVED));
            }
        });

        project.defineCompoundListener().onCreate().thenDo(e -> eventService.sendEvent(
                creatEvent(id, FEATURE_CREATED, e.getAffectedID()))).register();
        project.defineCompoundListener().onUpdate().thenDo(e -> eventService.sendEvent(
                creatEvent(id, FEATURE_UPDATED, e.getAffectedID()))).register();
        project.defineCompoundListener().onDelete().thenDo(e -> eventService.sendEvent(
                creatEvent(id, FEATURE_DELETED, e.getAffectedID()))).register();

        project.defineFormulaResultListener().onCreate().thenDo(e -> eventService.sendEvent(
                creatEvent(id, RESULT_CREATED, e.getAffectedID()))).register();
        project.defineFormulaResultListener().onUpdate().thenDo(e -> eventService.sendEvent(
                creatEvent(id, RESULT_UPDATED, e.getAffectedID()))).register();
        project.defineFormulaResultListener().onDelete().thenDo(e -> eventService.sendEvent(
                creatEvent(id, RESULT_DELETED, e.getAffectedID()))).register();

    }

    private ServerEventImpl<ProjectChangeEvent> creatEvent(
            String projectId,
            ProjectChangeEvent.Type eventType,
            FormulaResultId formulaResultId
    ) {
        CompoundContainerId compoundContainerId = formulaResultId.getParentId();
        return ServerEvents.newProjectEvent(
                ProjectChangeEvent.builder().eventType(eventType).projectId(projectId)
                        .compoundId(compoundContainerId.getGroupId().orElse(null))
                        .featuredId(compoundContainerId.getDirectoryName())
                        .formulaId(formulaResultId.fileName())
                        .build()
        );
    }

    private ServerEventImpl<ProjectChangeEvent> creatEvent(
            String projectId,
            ProjectChangeEvent.Type eventType,
            CompoundContainerId compoundContainerId
    ) {
        return ServerEvents.newProjectEvent(
                ProjectChangeEvent.builder().eventType(eventType).projectId(projectId)
                        .compoundId(compoundContainerId.getGroupId().orElse(null))
                        .featuredId(compoundContainerId.getDirectoryName())
                        .build()
        );
    }


    @Override
    public void destroy() {
        System.out.println("Destroy Project Provider Service...");
        closeAll();
        System.out.println("Destroy Project Provider Service DONE");

    }
}
