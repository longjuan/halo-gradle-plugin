package run.halo.gradle;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.DisableCachingByDefault;
import org.objectweb.asm.ClassReader;
import run.halo.gradle.utils.AsmConst;

@Slf4j
@DisableCachingByDefault(because = "Not worth caching")
public class PluginComponentsIndexTask extends DefaultTask {

    /**
     * The package separator character: {@code '.'}.
     */
    private static final char PACKAGE_SEPARATOR = '.';

    /**
     * The path separator character: {@code '/'}.
     */
    private static final char PATH_SEPARATOR = '/';

    private static final String CLASS_SUFFIX = ".class";
    private static final String FILEPATH = "META-INF/plugin-components.idx";

    public static final String TASK_NAME = "generatePluginComponentsIdx";

    @InputFiles
    ConfigurableFileCollection classesDirs = getProject().getObjects().fileCollection();

    @TaskAction
    public void generate() throws IOException {
        log.info("Generating plugin components index file...");

        String buildPath = classesDirs.getAsPath();
        Set<String> componentsIdxFileLines = new LinkedHashSet<>();
        componentsIdxFileLines.add("# Generated by Halo");
        for (File file : classesDirs.getAsFileTree()) {
            if (!file.getName().endsWith(CLASS_SUFFIX)) {
                continue;
            }

            ClassReader classReader = new ClassReader(new FileInputStream(file));
            FilterComponentClassVisitor filterComponentClassVisitor =
                new FilterComponentClassVisitor(AsmConst.ASM_VERSION);
            classReader.accept(filterComponentClassVisitor, ClassReader.SKIP_DEBUG);

            if (filterComponentClassVisitor.isComponentClass()) {
                String className =
                    convertResourcePathToClassName(filterComponentClassVisitor.getName());
                componentsIdxFileLines.add(className);
            }
        }
        // write to file
        Path componentsIdxPath = Paths.get(buildPath).resolve(FILEPATH);
        if (!Files.exists(componentsIdxPath.getParent())) {
            Files.createDirectories(componentsIdxPath.getParent());
        }
        if (!Files.exists(componentsIdxPath)) {
            Files.createFile(componentsIdxPath);
        }
        Files.write(componentsIdxPath, componentsIdxFileLines, StandardCharsets.UTF_8);
    }

    public ConfigurableFileCollection getClassesDirs() {
        return classesDirs;
    }

    /**
     * Convert a "/"-based resource path to a "."-based fully qualified class name.
     *
     * @param resourcePath the resource path pointing to a class
     * @return the corresponding fully qualified class name
     */
    public static String convertResourcePathToClassName(String resourcePath) {
        Assert.notNull(resourcePath, "Resource path must not be null");
        return resourcePath.replace(PATH_SEPARATOR, PACKAGE_SEPARATOR);
    }
}
