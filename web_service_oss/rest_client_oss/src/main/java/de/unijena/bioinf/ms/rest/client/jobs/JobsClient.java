/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
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
 *  You should have received a copy of the GNU General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.ms.rest.client.jobs;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.unijena.bioinf.ChemistryBase.utils.IOFunctions;
import de.unijena.bioinf.ms.properties.PropertyManager;
import de.unijena.bioinf.ms.rest.client.AbstractCsiClient;
import de.unijena.bioinf.ms.rest.model.JobId;
import de.unijena.bioinf.ms.rest.model.JobInputs;
import de.unijena.bioinf.ms.rest.model.JobTable;
import de.unijena.bioinf.ms.rest.model.JobUpdate;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.InputStreamEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

public class JobsClient extends AbstractCsiClient {
    private static final  int[] limits = new int[]{
            PropertyManager.getInteger("de.unijena.bioinf.sirius.http.job.fingerprint.limit", 500),
            PropertyManager.getInteger("de.unijena.bioinf.sirius.http.job.canopus.limit", 500),
            PropertyManager.getInteger("de.unijena.bioinf.sirius.http.job.covtree.limit", 500),
            PropertyManager.getInteger("de.unijena.bioinf.sirius.http.job.ftree.limit", 500)};
    @SafeVarargs
    public JobsClient(@Nullable URI serverUrl, @NotNull IOFunctions.IOConsumer<HttpUriRequest>... requestDecorator) {
        super(serverUrl, requestDecorator);
    }

    public EnumMap<JobTable, List<JobUpdate<?>>> getJobs(Collection<JobTable> jobTablesToCheck, @NotNull HttpClient client) throws IOException {
        return getJobs("/jobs/", jobTablesToCheck,client);
    }


    public EnumMap<JobTable, List<JobUpdate<?>>> getFinishedJobs(Collection<JobTable> jobTablesToCheck, @NotNull HttpClient client) throws IOException {
        return getJobs("/jobs-finished/", jobTablesToCheck,client);
    }

    public EnumMap<JobTable, List<JobUpdate<?>>> getJobs(@NotNull final String endpoint, Collection<JobTable> jobTablesToCheck, @NotNull HttpClient client) throws IOException {
        return executeFromJson(client,
                () -> new HttpGet(buildVersionSpecificWebapiURI(endpoint + CID)
                        .setParameter("limits", jobTablesToCheck.stream().sorted().map(s -> limits[s.ordinal()]).map(String::valueOf).collect(Collectors.joining(",")))
                        .setParameter("types", jobTablesToCheck.stream().sorted().map(JobTable::name).collect(Collectors.joining(",")))
                        .build()),
                new TypeReference<>() {}
        );
    }

    public EnumMap<JobTable, List<JobUpdate<?>>> postJobs(JobInputs submission, @NotNull HttpClient client) throws IOException {
        return executeFromJson(client,
                () -> {
                    HttpPost post = new HttpPost(buildVersionSpecificWebapiURI("/jobs/" + CID).build());
                    ObjectMapper om = new ObjectMapper();
                    om.setSerializationInclusion(JsonInclude.Include.NON_NULL);

                    post.setEntity(new InputStreamEntity(new ByteArrayInputStream(
                            om.writeValueAsBytes(submission)), ContentType.APPLICATION_JSON));
                    return post;
                },
                new TypeReference<>() {}
        );
    }

    /**
     * Unregisters Client and deletes all its jobs on server
     */
    public void deleteAllJobs(@NotNull HttpClient client) throws IOException {
        execute(client, () -> new HttpPatch(buildVersionSpecificWebapiURI("/jobs/" + CID + "/delete").build()));
    }


    public void deleteJobs(Collection<JobId> jobsToDelete, Map<JobId, Integer> countingHashes, @NotNull HttpClient client) throws IOException {
        execute(client, () -> {
            Map<String, String> body = new HashMap<>();
            body.put("jobs", new ObjectMapper().writeValueAsString(jobsToDelete));
            if (countingHashes != null && !countingHashes.isEmpty()) //add client sided counting if available
                body.put("countingHashes", new ObjectMapper().writeValueAsString(countingHashes));

            HttpPatch patch = new HttpPatch(buildVersionSpecificWebapiURI("/jobs/" + CID + "/delete").build());
            patch.setEntity(new InputStreamEntity(new ByteArrayInputStream(
                    new ObjectMapper().writeValueAsBytes(body)), ContentType.APPLICATION_JSON));
            return patch;
        });
    }
}
