package io.vokal.gradle.resgen

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.ProjectConfigurationException

import groovy.json.*

import java.io.File;
import java.io.FileInputStream;
import java.io.RandomAccessFile;
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import javax.imageio.ImageIO;

import javax.inject.Inject

class MocktrofitPlugin implements Plugin<Project> {
    Project project;
    void apply(Project project) {

        this.project = project
        def hasApp = project.hasProperty('android')
        def root = project.getProjectDir().getAbsolutePath();
        def fs = FileSystems.getDefault();

        project.extensions.create("mocktrofit", MocktrofitPlugin)
        project.android.variants.each { variant ->
            def tname = "clear${variant.getName()}Cache"
            project.tasks.create(name: tname) << { 
                //project.android.sourceSets.each { source ->
                    //Path srcPath =  fs.getPath(root, "src", source.name)
                    //Path path = fs.getPath(srcPath.toString(), ".res-gen")
                    //Path cache = FileSystems.getDefault().getPath(srcPath.toString(), "res-pdf", ".cache");

                    //try { Files.deleteIfExists(cache); }
                    //catch(IOException e) { [> ... <] }

                    //if(Files.exists(path)) {
                        //deleteRecursively(path)
                    //}
                //}
            }
            project.tasks[tname].dependsOn "merge${variant.getName()}Assets"
            project.tasks["preBuild"].dependsOn tname
        }
    }

    private void deleteRecursively(Path path) {
        Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    if (exc == null) {
                        Files.delete(dir);
                        return FileVisitResult.CONTINUE;
                    } else {
                        // directory iteration failed; propagate exception
                        throw exc;
                    }
                }
            });
    }


    private Path createFolder(Path path, String density) {
        Path folder = FileSystems.getDefault().getPath(path.toString(), "drawable-" + density);
        if (Files.notExists(folder)) Files.createDirectories(folder)
        return folder;
    }

    def filtered(map, densities) {
        return map.findAll { densities.contains(it.key) }
    }
}

class MocktrofitExtension {
    String mockFolder = "mocks"
}
