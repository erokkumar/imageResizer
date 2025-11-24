package com.smartresizer.smartresizer.controller;

import com.smartresizer.smartresizer.model.ImageInfo;
import com.smartresizer.smartresizer.service.ImageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

@Controller
public class ImageController {

    @Autowired
    private ImageService imageService;

    @GetMapping("/")
    public String home() {
        return "index";
    }

    @PostMapping("/resize")
    public String resizeImage(
            @RequestParam("image") MultipartFile file,
            @RequestParam(required = false) Integer width,
            @RequestParam(required = false) Integer height,
            @RequestParam(required = false) Integer targetKB,
            @RequestParam(defaultValue = "jpg") String format,
            Model model
    ) throws Exception {

        // ✅ Save uploaded file temporarily
        File temp = File.createTempFile("upload_", file.getOriginalFilename());
        file.transferTo(temp);

        ImageInfo info;

        // ✅ Decide mode
        if (targetKB != null && targetKB > 0) {
            info = imageService.compressByTargetSize(temp, targetKB, format);
        } else if (width != null && height != null && width > 0 && height > 0) {
            info = imageService.resizeByDimensions(temp, width, height, format);
        } else {
            throw new IllegalArgumentException("Please enter either width/height or target size (KB)");
        }

        model.addAttribute("info", info);
        return "result";
    }

    @PostMapping("/suggest")
    @ResponseBody
    public Map<String, Object> suggestResize(@RequestParam("image") MultipartFile file) throws Exception {
        File temp = File.createTempFile("upload_", file.getOriginalFilename());
        file.transferTo(temp);
        return imageService.suggestResizeParameters(temp);
    }


    @GetMapping("/download")
    public ResponseEntity<FileSystemResource> download(@RequestParam String filename) throws Exception {
        // ✅ Build safe internal path (no external path exposure)
        Path path = Path.of(System.getProperty("user.dir"), "uploads", filename);
        File file = path.toFile();

        if (!file.exists()) {
            return ResponseEntity.notFound().build();
        }

        // ✅ Detect content type automatically
        String contentType = Files.probeContentType(path);
        if (contentType == null) {
            contentType = "application/octet-stream";
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + file.getName())
                .contentType(MediaType.parseMediaType(contentType))
                .body(new FileSystemResource(file));
    }
}
