/*
 * Copyright (C) 2014 Trillian Mobile AB.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.robovm.compilerhelper;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;

import java.io.*;

public class Archiver {

    public static void unarchive(File archiveFile, File destinationDirectory) throws IOException {

        TarArchiveInputStream tar = new TarArchiveInputStream(new GzipCompressorInputStream(new BufferedInputStream(
                new FileInputStream(archiveFile))));

        destinationDirectory.mkdirs();
        TarArchiveEntry entry = tar.getNextTarEntry();
        while (entry != null) {
            File f = new File(destinationDirectory, entry.getName());
            if (entry.isDirectory()) {
                f.mkdirs();
                entry = tar.getNextTarEntry();
                continue;
            }

            String parentDir = f.getPath();
            if (parentDir != null) {
                new File(parentDir.substring(0, parentDir.lastIndexOf(File.separator))).mkdirs();
            }

            f.createNewFile();
            byte[] bytes = new byte[1024];
            int count;
            BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(f));
            while ((count = tar.read(bytes)) > 0) {
                out.write(bytes, 0, count);
            }
            out.flush();
            out.close();

            entry = tar.getNextTarEntry();
        }
    }
}
