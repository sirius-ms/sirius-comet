/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
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
 *  You should have received a copy of the GNU Lesser General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.storage.blob.minio;

import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.ChemistryBase.utils.IOFunctions;
import de.unijena.bioinf.jjobs.TinyBackgroundJJob;
import de.unijena.bioinf.storage.blob.BlobStorage;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.errors.*;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.file.Path;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ExecutionException;

public class MinIoBlobStorage implements BlobStorage {

    private final MinioClient minioClient;
    private final String bucketName;


    public MinIoBlobStorage(@NotNull String bucketName, @NotNull MinioClient client) {
        this.minioClient = client;
        this.bucketName = bucketName;
        init();
    }

    private void init() {
        if (!MinIoUtils.bucketExists(bucketName, minioClient))
            throw new IllegalArgumentException("Database bucket seems to be not existent or you have not the correct permissions");
    }

    @Override
    public String getName() {
        return bucketName;
    }

    @Override
    public boolean hasBlob(Path relative) throws IOException {
        try {
            return minioClient.getObject(GetObjectArgs.builder().bucket(bucketName).object(relative.toString()).build()).available() > 0;
        } catch (ErrorResponseException | InvalidResponseException | IOException | InsufficientDataException | InternalException | InvalidKeyException | NoSuchAlgorithmException | ServerException | XmlParserException e) {
            throw new IOException("Error when Searching object", e);
        }
    }

    @Override
    public OutputStream writer(Path relative) throws IOException {
        return BackgroundPipedOutputStream.createAndRead((in) -> {
            try {
                return minioClient.putObject(PutObjectArgs.builder().bucket(bucketName).object(makePath(relative)).stream(in, -1, 100 * 1024 * 1024).build());
            } catch (ErrorResponseException | InvalidResponseException | IOException | InsufficientDataException | InternalException | InvalidKeyException | NoSuchAlgorithmException | ServerException | XmlParserException e) {
                throw new IOException(e);
            }
        });
    }

    @Override
    public InputStream reader(Path relative) throws IOException {
        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(makePath(relative)).build());
        } catch (ErrorResponseException | InvalidResponseException | IOException | InsufficientDataException | InternalException | InvalidKeyException | NoSuchAlgorithmException | ServerException | XmlParserException e) {
            throw new IOException("Error when Searching Object", e);
        }
    }

    private String makePath(Path relative) {
        return relative.toString();
    }

    private static class BackgroundPipedOutputStream<R> extends PipedOutputStream {
        private TinyBackgroundJJob<R> readJob;


        public BackgroundPipedOutputStream() {
            super();
        }

        public void readInBackground(IOFunctions.IOFunction<PipedInputStream, R> readSink) throws IOException {
            final PipedInputStream in = new PipedInputStream(this);
            readJob = SiriusJobs.runInBackground(() -> {
                try {
                    return readSink.apply(in);
                } finally {
                    in.close();
                }
            });
        }

        public R awaitReadInBackground() throws ExecutionException {
            if (readJob == null)
                return null;
            return readJob.awaitResult();
        }

        @Override
        public void close() throws IOException {
            super.close();
            try {
                awaitReadInBackground();
            } catch (ExecutionException e) {
                throw new IOException(e);
            }
        }

        public static <R> BackgroundPipedOutputStream<R> createAndRead(IOFunctions.IOFunction<PipedInputStream, R> readSink) throws IOException {
            BackgroundPipedOutputStream<R> s = new BackgroundPipedOutputStream<>();
            s.readInBackground(readSink);
            return s;
        }
    }
}
