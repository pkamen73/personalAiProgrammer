package de.itsourcerer.aiideassistant.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/fs")
public class FsController {

    @GetMapping("/dirs")
    public ResponseEntity<List<DirEntry>> listDirs(@RequestParam(required = false) String path) {
        List<DirEntry> entries;

        if (path == null || path.isBlank()) {
            entries = Arrays.stream(File.listRoots())
                    .map(r -> new DirEntry(r.getAbsolutePath(), r.getAbsolutePath(), true))
                    .collect(Collectors.toList());
        } else {
            File dir = new File(path);
            if (!dir.isDirectory()) {
                return ResponseEntity.badRequest().build();
            }
            File[] children = dir.listFiles(f -> f.isDirectory() && !f.isHidden());
            if (children == null) {
                return ResponseEntity.ok(Collections.emptyList());
            }
            entries = Arrays.stream(children)
                    .sorted((a, b) -> a.getName().compareToIgnoreCase(b.getName()))
                    .map(f -> new DirEntry(f.getName(), f.getAbsolutePath(), hasSubDirs(f)))
                    .collect(Collectors.toList());
        }

        return ResponseEntity.ok(entries);
    }

    private boolean hasSubDirs(File dir) {
        File[] children = dir.listFiles(f -> f.isDirectory() && !f.isHidden());
        return children != null && children.length > 0;
    }

    public record DirEntry(String name, String path, boolean hasChildren) {}
}
