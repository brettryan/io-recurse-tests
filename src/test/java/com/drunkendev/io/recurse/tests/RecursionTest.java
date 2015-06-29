/*
 * RecursionTests.java    Jun 27 2015, 21:41
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 Brett Ryan
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.drunkendev.io.recurse.tests;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.junit.Before;
import org.junit.Test;

import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;
import static org.junit.Assert.fail;


/**
 * Tests to determine performance and symbolic-link behavior of different file
 * traversal methods.
 *
 * I have decided to implement a number of tests that are called by a repeated
 * test method in order to determine the average run length over an aggregate
 * number of repeated calls.
 *
 * @author  Brett Ryan
 * @see     <a href="http://stackoverflow.com/questions/2056221/recursively-list-files-in-java/24006711?noredirect=1#comment50144003_24006711">Stack Overflow question: Recursively list files in Java</a>
 */
public class RecursionTest {

    private Path startPath;

    @Before
    public void setUp() {
        // Defaults to current directory, replace with any path you desire to
        // test with.
        startPath = Paths.get("").normalize().toAbsolutePath();
    }

    /**
     * Performs an averaging test against each method.
     *
     * Note: For the most un-biasing test, run only one test per execution. This
     * will ensure that prior tests are less likely to have influence on your
     * test results.
     *
     * To further un-bias the tests, run a priming test first with a count of 1
     * and disregard it's result.
     */
    @Test
    public void averageTest() {
        int testCount = 5;
        averageTest(testCount, () -> testWalkFileTree());
        averageTest(testCount, () -> testJava8StreamSequential());
        averageTest(testCount, () -> testJava8StreamParallel());
        averageTest(testCount, () -> testFileWalker());
        averageTest(testCount, () -> testFileUtils());
        averageTest(testCount, () -> testListFilesRecursive());
        averageTest(testCount, () -> testQueue());
    }

    /**
     * Answer provided by yawn.
     *
     * This method uses a {@link FileVisitor} implementation that counts files
     * and directories.
     *
     * This test uses NIO {@link Files#walkFileTree(Path, FileVisitor)}.
     *
     * @see     <a href="http://stackoverflow.com/a/2056352/140037">Stack-Overflow answer by yawn</a>
     */
//    @Test
    public void testWalkFileTree() {
        System.out.println("\nTEST: Walk File Tree");
        time(() -> {
            PathCounterFileVisitor counter = new PathCounterFileVisitor();
            try {
                Files.walkFileTree(startPath, counter);
            } catch (IOException ex) {
                fail(ex.getMessage());
            }
            System.out.format("Files: %d, dirs: %d. ", counter.getFiles(), counter.getDirs());
        });
    }

    /**
     * Answer provided by Brett Ryan.
     *
     * This test uses Java-8's java {@link java.util.stream.Stream Stream API}
     * by {@link Files#walk}.
     *
     * This is the sequential version.
     *
     * @see     <a href="http://stackoverflow.com/a/24006711/140037">Stack-Overflow answer by Brett Ryan</a>
     */
//    @Test
    public void testJava8StreamSequential() {
        System.out.println("\nTEST: Java 8 Stream Sequential");
        time(() -> {
            try {
                Map<Integer, Long> stats = Files.walk(startPath)
                        .sequential()
                        .collect(groupingBy(n -> Files.isDirectory(n, LinkOption.NOFOLLOW_LINKS) ? 1 : 2, counting()));
                System.out.format("Files: %d, dirs: %d. ", stats.get(2), stats.get(1));
            } catch (IOException ex) {
                fail(ex.getMessage());
            }
        });
    }

    /**
     * Answer provided by Brett Ryan.
     *
     * This test uses Java-8's java {@link java.util.stream.Stream Stream API}
     * by {@link Files#walk}.
     *
     * This is the parallel version.
     *
     * @see     <a href="http://stackoverflow.com/a/24006711/140037">Stack-Overflow answer by Brett Ryan</a>
     */
//    @Test
    public void testJava8StreamParallel() {
        System.out.println("\nTEST: Java 8 Stream Parallel");
        time(() -> {
            try {
                Map<Integer, Long> stats = Files.walk(startPath)
                        .parallel()
                        .collect(groupingBy(n -> Files.isDirectory(n, LinkOption.NOFOLLOW_LINKS) ? 1 : 2, counting()));
                System.out.format("Files: %d, dirs: %d. ", stats.get(2), stats.get(1));
            } catch (IOException ex) {
                fail(ex.getMessage());
            }
        });
    }

    /**
     * Answer provided by stacker.
     *
     * Tests a custom {@link FileWalker} that uses the {@link java.io.File} API.
     *
     * @see     <a href="http://stackoverflow.com/a/2056326/140037">Stack-Overflow answer by stacker</a>
     */
//    @Test
    public void testFileWalker() {
        System.out.println("\nTEST: File Walker");
        Filewalker fw = new Filewalker();
        time(() -> {
            try {
                fw.walk(startPath.toAbsolutePath().toString());
                System.out.format("Files: %d, dirs: %d. ", fw.getFiles(), fw.getDirs());
            } catch (IOException ex) {
                fail(ex.getMessage());
            }
        });
    }

    /**
     * Answer provided by Bozho.
     *
     * Tests using <a href="https://commons.apache.org/proper/commons-io/">Apache commons-io</a>
     * {@link FileUtils#iterateFilesAndDirs(File, IOFileFilter, IOFileFilter)}
     * which uses the {@link java.io.File} API.
     *
     * @see     <a href="http://stackoverflow.com/a/2056258/140037">Stack-Overflow answer by Bozho</a>
     */
//    @Test
    public void testFileUtils() {
        System.out.println("\nTEST: commons-io - FileUtils");
        time(() -> {
            Iterator<File> iter = FileUtils.iterateFilesAndDirs(startPath.toFile(),
                                                                TrueFileFilter.INSTANCE,
                                                                new IOFileFilter() {
                                                                    @Override
                                                                    public boolean accept(File file) {
                                                                        try {
                                                                            return isPlainDir(file);
                                                                        } catch (IOException ex) {
                                                                            return false;
                                                                        }
                                                                    }

                                                                    @Override
                                                                    public boolean accept(File dir, String name) {
                                                                        try {
                                                                            return isPlainDir(dir);
                                                                        } catch (IOException ex) {
                                                                            return false;
                                                                        }
                                                                    }
                                                                });
            int files = 0;
            int dirs = 0;

            File n;
            try {
                while (iter.hasNext()) {
                    n = iter.next();
                    if (isPlainDir(n)) {
                        dirs++;
                    } else {
                        files++;
                    }
                }
                System.out.format("Files: %d, dirs: %d. ", files, dirs);
            } catch (IOException ex) {
                fail(ex.getMessage());
            }
        });

    }

    /**
     * Answer provided by Stefan Schmidt.
     *
     * Uses a recursive calling function with the {@link java.io.File} API.
     *
     * @see     <a href="http://stackoverflow.com/a/2056276/140037">Stack-Overflow answer by Stefan Schmidt</a>
     */
//    @Test
    public void testListFilesRecursive() {
        System.out.println("\nTEST: listFiles - Recursive");
        Filewalker fw = new Filewalker();
        time(() -> {
            try {
                int[] res = new int[]{0, 1}; // count initial directory.
                countRecursive(startPath.toFile(), res);
                System.out.format("Files: %d, dirs: %d. ", res[0], res[1]);
            } catch (IOException ex) {
                fail(ex.getMessage());
            }
        });
    }

    /**
     * Function to recursively count files and directories.
     *
     * Note: This method will not count the initial directory, to include it
     * initialize {@code res} with {@code {0, 1}}.
     *
     * @param   file
     *          Initial {@link File} to recurse from.
     * @param   res
     *          Array containing {@code res[0]} = files, {@code res[1] directories}.
     * @throws  IOException
     *          If a symbolic link could not be determined. This is ultimately
     *          caused by a call to {@link File#getCanonicalFile()}.
     */
    public void countRecursive(File file, int[] res) throws IOException {
        File[] children = file.listFiles();
        for (File child : children) {
            if (isPlainDir(child)) {
                res[1]++;
                countRecursive(child, res);
            } else {
                res[0]++;
            }
        }
    }

    /**
     * Answer provided by benroth.
     *
     * Uses a {@link Queue} to hold directory references while traversing until
     * the queue becomes empty. Uses the {@link java.io.File} API.
     *
     * @see     <a href="http://stackoverflow.com/a/10814316/140037">Stack-Overflow answer by benroth</a>
     */
//    @Test
    public void testQueue() {
        System.out.println("\nTEST: listFiles - Queue");
        time(() -> {
            Queue<File> dirsq = new LinkedList<>();
            dirsq.add(startPath.toFile());
            int files = 0;
            int dirs = 0;
            try {
                dirs++; // to count the initial dir.
                while (!dirsq.isEmpty()) {
                    for (File f : dirsq.poll().listFiles()) {
                        if (isPlainDir(f)) {
                            dirsq.add(f);
                            dirs++;
                        } else if (f.isFile()) {
                            files++;
                        }
                    }
                }
                System.out.format("Files: %d, dirs: %d. ", files, dirs);
            } catch (IOException ex) {
                fail(ex.getMessage());
            }
        });
    }

    /**
     * Used to perform a timed average of a repeated set of runs.
     *
     * @param   count
     *          Amount of times to run {@code r}.
     * @param   r
     *          {@link Runnable} object to perform tests against.
     */
    public void averageTest(int count, Runnable r) {
        Duration total = Duration.ZERO;
        for (int i = 0; i < count; i++) {
            total = total.plus(time(() -> r.run()));
        }
        System.out.format("%nAverage duration: %s%n%n", total.dividedBy(count));
    }

    /**
     * Times a {@link Runnable} instance.
     *
     * @param   r
     *          {@link Runnable} object to time.
     * @return  {@link Duration} object containing run-time length.
     */
    public Duration time(Runnable r) {
        Instant start = Instant.now();
        r.run();
        Duration dur = Duration.between(start, Instant.now());
        System.out.format("Completed in: %s%n", dur.toString());
        return dur;
    }


    /**
     * A {@link FileVisitor} implementation that counts files and directories.
     */
    private static final class PathCounterFileVisitor extends SimpleFileVisitor<Path> {

        private int files;
        private int dirs;

        /**
         * Count of all files found within this visitor.
         *
         * @return  Count of files found.
         */
        public int getFiles() {
            return files;
        }

        /**
         * Count of all directories found within this visitor.
         *
         * @return  Count of directories found.
         */
        public int getDirs() {
            return dirs;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            files++;
            return FileVisitResult.CONTINUE;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException e) throws IOException {
            if (e == null) {
                dirs++;
                return FileVisitResult.CONTINUE;
            } else {
                throw e;
            }
        }

    }


    /**
     * File walker implementation to count files using {@link java.io}.
     */
    public class Filewalker {

        private int files;
        private int dirs = 1; // to count initial dir.

        /**
         * Count of all files found within this walker.
         *
         * @return  Count of files found.
         */
        public int getFiles() {
            return files;
        }

        /**
         * Count of all directories found within this walker.
         *
         * @return  Count of directories found.
         */
        public int getDirs() {
            return dirs;
        }

        /**
         * Walk the file tree.
         *
         * @param   path
         *          Path to walk.
         * @throws  IOException
         *          If a symbolic link could not be determined. This is ultimately
         *          caused by a call to {@link File#getCanonicalFile()}.
         */
        public void walk(String path) throws IOException {
            File root = new File(path);
            File[] list = root.listFiles();

            if (list == null) {
                return;
            }

            for (File f : list) {
                if (isPlainDir(f)) {
                    walk(f.getAbsolutePath());
                    dirs++;
                } else {
                    files++;
                }
            }
        }

    }

    /**
     * Determine if {@code file} is a directory and is not a symbolic link.
     *
     * @param   file
     *          File to test.
     * @return  True if {@code file} is a directory and is not a symbolic link.
     * @throws  IOException
     *          If a symbolic link could not be determined. This is ultimately
     *          caused by a call to {@link File#getCanonicalFile()}.
     */
    private static boolean isPlainDir(File file) throws IOException {
        return file.isDirectory() && !isSymbolicLink(file);
    }

    /**
     * Given a {@link File} object, test if it is likely to be a symbolic link.
     *
     * @param   file
     *          File to test for symbolic link.
     * @return  {@code true} if {@code file} is a symbolic link.
     * @throws  NullPointerException
     *          If {@code file} is null.
     * @throws  IOException
     *          If a symbolic link could not be determined. This is ultimately
     *          caused by a call to {@link File#getCanonicalFile()}.
     */
    private static boolean isSymbolicLink(File file) throws IOException {
        if (file == null) {
            throw new NullPointerException("File must not be null");
        }
        File canon;
        if (file.getParent() == null) {
            canon = file;
        } else {
            File canonDir = file.getParentFile().getCanonicalFile();
            canon = new File(canonDir, file.getName());
        }
        return !canon.getCanonicalFile().equals(canon.getAbsoluteFile());
    }

}
