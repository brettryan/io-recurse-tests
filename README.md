# io-recurse-tests

Benchmarking tests for variations of recursively listing all I/O files.

All tests were performed using 5 iterations alone on each pass with an initial
priming run to ensure that results are not influenced by prior test runs.

Tests were performed on a MacBook Pro with OSX 10.10.3 with 32GB RAM.

Path examined contained 383,378 files, 101,854 directories and 2 sym-links which
when included with the methods that work with them are treated as files.

| Test Name                 | API | Average Time  | Sym-Links Visited | Initial Directory Visited | SO Answer                                                   |
|---------------------------|-----|---------------|-------------------|---------------------------|-------------------------------------------------------------|
| Java 8 Stream Parallel    | NIO | 28.425S       | Yes               | Yes                       | [Brett Ryan](http://stackoverflow.com/a/24006711/140037)    |
| Walk File Tree            | NIO | 28.4718S      | Yes               | Yes                       | [yawn](http://stackoverflow.com/a/2056352/140037)           |
| Java 8 Stream Sequential  | NIO | 29.62S        | Yes               | Yes                       | [Brett Ryan](http://stackoverflow.com/a/24006711/140037)    |
| File Walker               | I/O | 32.5256S      | Yes               | No                        | [stacker](http://stackoverflow.com/a/2056326/140037)        |
| listFiles - Recursive     | I/O | 32.5864S      | Yes               | No                        | [Stefan Schmidt](http://stackoverflow.com/a/2056276/140037) |
| listFiles - Queue         | I/O | 33.7566S      | No                | Yes                       | [benroth](http://stackoverflow.com/a/10814316/140037)       |
| commons-io - FileUtils    | I/O | 1M5.413S      | No                | Yes                       | [Bozho](http://stackoverflow.com/a/2056258/140037)          |

My conclusion is to use a stream API if you are able to use Java-8, this will
improve over subsequent releases and with a parallel stream is almost the same
timing as the Java-7 NIO walk alternative.

If you cannot use Java-8, then stick with an NIO variant. The tests performed
are the same as with a Java-8 Parallel stream.

If you must remain Java-6 and prior compatible, use the file walker method if
possible, this is fairly flexible. The only caveat is that the initial directory
is not visited.

Special care needs to be taken with many of the I/O API alternatives with
relation to symbolic links, as in the old API there is no built-in for
determining if a File is a symbolic-link or not. If the link is followed and the
link happens to be of a parent folder you will end up in a recursive loop.

