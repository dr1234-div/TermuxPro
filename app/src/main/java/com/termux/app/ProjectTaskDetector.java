package com.termux.app;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

/** 将只读项目元数据转换为可确认执行的常用任务。 */
final class ProjectTaskDetector {

    private ProjectTaskDetector() {}

    @NonNull
    static ProjectInfo parse(@NonNull String output) throws JSONException {
        String[] fields = output.split("\u0000", 3);
        String kind = fields.length == 0 ? "UNKNOWN" : fields[0];
        if ("PACKAGE_JSON".equals(kind) && fields.length == 3) {
            return parsePackageJson(fields[1], fields[2]);
        }
        if ("MAVEN_WRAPPER".equals(kind)) return javaProject("Maven", "./mvnw", true);
        if ("MAVEN".equals(kind)) return javaProject("Maven", "mvn", true);
        if ("GRADLE_WRAPPER".equals(kind)) return javaProject("Gradle", "./gradlew", false);
        if ("GRADLE".equals(kind)) return javaProject("Gradle", "gradle", false);
        return new ProjectInfo("未知项目", Collections.emptyList());
    }

    @NonNull
    private static ProjectInfo parsePackageJson(String manager, String json) throws JSONException {
        JSONObject root = new JSONObject(json);
        JSONObject dependencies = root.optJSONObject("dependencies");
        JSONObject devDependencies = root.optJSONObject("devDependencies");
        String type;
        if (has(dependencies, "@dcloudio/uni-app") || has(devDependencies, "@dcloudio/uni-app")) {
            type = "uni-app · " + manager;
        } else if (has(dependencies, "vue") || has(devDependencies, "vue")) {
            type = "Vue · " + manager;
        } else if (has(dependencies, "react") || has(devDependencies, "react")) {
            type = "React · " + manager;
        } else {
            type = "Node.js · " + manager;
        }

        List<Task> tasks = new ArrayList<>();
        JSONObject scripts = root.optJSONObject("scripts");
        if (scripts != null) {
            Iterator<String> names = scripts.keys();
            while (names.hasNext() && tasks.size() < 40) {
                String name = names.next();
                tasks.add(new Task(name, manager + " run " + WorkspaceCommandBuilder.shellQuote(name)));
            }
            Collections.sort(tasks, Comparator
                .comparingInt((Task task) -> priority(task.label))
                .thenComparing(task -> task.label));
        }
        return new ProjectInfo(type, tasks);
    }

    private static boolean has(JSONObject object, String name) {
        return object != null && object.has(name);
    }

    private static int priority(String name) {
        String[] preferred = {"dev", "start", "build", "test", "lint", "typecheck", "type-check"};
        for (int index = 0; index < preferred.length; index++) {
            if (preferred[index].equals(name)) return index;
        }
        return preferred.length;
    }

    @NonNull
    private static ProjectInfo javaProject(String type, String executable, boolean maven) {
        List<Task> tasks = new ArrayList<>();
        if (maven) {
            tasks.add(new Task("test", executable + " test"));
            tasks.add(new Task("package（跳过测试）", executable + " package -DskipTests"));
            tasks.add(new Task("Spring Boot 启动", executable + " spring-boot:run"));
        } else {
            tasks.add(new Task("test", executable + " test"));
            tasks.add(new Task("build", executable + " build"));
            tasks.add(new Task("Spring Boot 启动", executable + " bootRun"));
        }
        return new ProjectInfo(type, tasks);
    }

    static final class ProjectInfo {
        final String type;
        final List<Task> tasks;

        ProjectInfo(String type, List<Task> tasks) {
            this.type = type;
            this.tasks = tasks;
        }
    }

    static final class Task {
        final String label;
        final String command;

        Task(String label, String command) {
            this.label = label;
            this.command = command;
        }

        @NonNull
        @Override
        public String toString() {
            return label + "\n" + command;
        }
    }
}
