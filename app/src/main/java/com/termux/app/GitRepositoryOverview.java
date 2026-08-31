package com.termux.app;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Git 工作台只读概览；只解析 TermuxPro 自有协议，不解释面向人的 Git 输出。 */
final class GitRepositoryOverview {

    private static final String OVERVIEW = "TP_OVERVIEW\t";
    private static final String LOCAL_BRANCH = "TP_LOCAL\t";
    private static final String REMOTE_BRANCH = "TP_REMOTE\t";
    private static final String LOG = "TP_LOG\t";

    @NonNull final String head;
    final boolean detached;
    final int changedFiles;
    @Nullable final Integer ahead;
    @Nullable final Integer behind;
    @NonNull final List<String> localBranches;
    @NonNull final List<String> remoteBranches;
    @NonNull final List<Commit> commits;

    private GitRepositoryOverview(@NonNull String head, boolean detached, int changedFiles,
                                  @Nullable Integer ahead, @Nullable Integer behind,
                                  @NonNull List<String> localBranches,
                                  @NonNull List<String> remoteBranches,
                                  @NonNull List<Commit> commits) {
        this.head = head;
        this.detached = detached;
        this.changedFiles = changedFiles;
        this.ahead = ahead;
        this.behind = behind;
        this.localBranches = Collections.unmodifiableList(localBranches);
        this.remoteBranches = Collections.unmodifiableList(remoteBranches);
        this.commits = Collections.unmodifiableList(commits);
    }

    @NonNull
    static GitRepositoryOverview parse(@NonNull String output) {
        String head = null;
        boolean detached = false;
        int changedFiles = 0;
        Integer ahead = null;
        Integer behind = null;
        List<String> local = new ArrayList<>();
        List<String> remote = new ArrayList<>();
        List<Commit> commits = new ArrayList<>();

        for (String line : output.split("\n")) {
            if (line.startsWith(OVERVIEW)) {
                String[] fields = line.split("\t", -1);
                if (fields.length != 7) throw new IllegalArgumentException("Invalid Git overview record");
                head = requireValue(fields[1], "head");
                detached = "1".equals(fields[2]);
                changedFiles = parseNonNegative(fields[3], "changed files");
                if (!fields[4].isEmpty()) ahead = parseNonNegative(fields[4], "ahead");
                if (!fields[5].isEmpty()) behind = parseNonNegative(fields[5], "behind");
                if (!"1".equals(fields[6])) ahead = behind = null;
            } else if (line.startsWith(LOCAL_BRANCH)) {
                local.add(requireValue(line.substring(LOCAL_BRANCH.length()), "local branch"));
            } else if (line.startsWith(REMOTE_BRANCH)) {
                remote.add(requireValue(line.substring(REMOTE_BRANCH.length()), "remote branch"));
            } else if (line.startsWith(LOG)) {
                String[] fields = line.split("\t", 4);
                if (fields.length != 4) throw new IllegalArgumentException("Invalid Git log record");
                commits.add(new Commit(requireValue(fields[1], "commit"), fields[2], fields[3]));
            }
        }
        if (head == null) throw new IllegalArgumentException("Missing Git overview record");
        return new GitRepositoryOverview(head, detached, changedFiles, ahead, behind,
            local, remote, commits);
    }

    private static int parseNonNegative(@NonNull String value, @NonNull String name) {
        try {
            int parsed = Integer.parseInt(value);
            if (parsed < 0) throw new NumberFormatException();
            return parsed;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Invalid " + name);
        }
    }

    @NonNull
    private static String requireValue(@NonNull String value, @NonNull String name) {
        if (value.isEmpty()) throw new IllegalArgumentException("Missing " + name);
        return value;
    }

    static final class Commit {
        @NonNull final String shortHash;
        @NonNull final String relativeTime;
        @NonNull final String subject;

        Commit(@NonNull String shortHash, @NonNull String relativeTime, @NonNull String subject) {
            this.shortHash = shortHash;
            this.relativeTime = relativeTime;
            this.subject = subject;
        }
    }
}
