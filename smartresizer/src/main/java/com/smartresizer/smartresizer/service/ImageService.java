package com.smartresizer.smartresizer.service;

import com.smartresizer.smartresizer.model.ImageInfo;
import org.springframework.stereotype.Service;

import javax.imageio.*;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

@Service
public class ImageService {

    private final Path uploadDir = Path.of(System.getProperty("user.dir"), "uploads");

    public ImageService() throws IOException {
        if (!Files.exists(uploadDir)) Files.createDirectories(uploadDir);
    }

    public Map<String, Object> suggestResizeParameters(File imageFile) throws IOException {
        BufferedImage img = ImageIO.read(imageFile);
        long sizeKB = imageFile.length() / 1024;
        int width = img.getWidth();
        int height = img.getHeight();

        int suggestedWidth = width;
        int suggestedHeight = height;
        int suggestedKB = (int) Math.min(sizeKB * 0.4, 500);

        if (width > 2000 || height > 2000) {
            suggestedWidth = width / 2;
            suggestedHeight = height / 2;
        } else if (sizeKB > 1000) {
            suggestedWidth = (int) (width * 0.7);
            suggestedHeight = (int) (height * 0.7);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("originalWidth", width);
        result.put("originalHeight", height);
        result.put("originalSizeKB", sizeKB);
        result.put("suggestedWidth", suggestedWidth);
        result.put("suggestedHeight", suggestedHeight);
        result.put("suggestedKB", suggestedKB);
        return result;
    }

    public ImageInfo resizeByDimensions(File originalFile, int newW, int newH, String format) throws IOException {
        BufferedImage original = ImageIO.read(originalFile);
        BufferedImage resized = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_RGB);

        Graphics2D g = resized.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(original, 0, 0, newW, newH, Color.WHITE, null);
        g.dispose();

        String newName = "resized_" + UUID.randomUUID() + "." + format.toLowerCase();
        File outFile = uploadDir.resolve(newName).toFile();
        ImageIO.write(resized, format, outFile);

        ImageInfo info = new ImageInfo();
        info.setFileName(originalFile.getName());
        info.setResizedFilePath(outFile.getAbsolutePath());
        info.setOriginalWidth(original.getWidth());
        info.setOriginalHeight(original.getHeight());
        info.setOriginalSizeKB(originalFile.length() / 1024);
        info.setNewWidth(newW);
        info.setNewHeight(newH);
        info.setNewSizeKB(outFile.length() / 1024);
        info.setResizedFileName(outFile.getName());
        return info;
    }

    public ImageInfo compressByTargetSize(File originalFile, int targetKB, String format) throws IOException {
        BufferedImage original = ImageIO.read(originalFile);
        String newName = "resized_" + UUID.randomUUID() + "." + format.toLowerCase();
        File outFile = uploadDir.resolve(newName).toFile();

        long originalKB = originalFile.length() / 1024;
        double ratio = Math.sqrt((double) targetKB / originalKB);

        int newW = Math.max(1, (int) (original.getWidth() * ratio));
        int newH = Math.max(1, (int) (original.getHeight() * ratio));

        BufferedImage resized = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = resized.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(original, 0, 0, newW, newH, Color.WHITE, null);
        g.dispose();

        float quality = 0.9f;
        byte[] lastValidBytes = null;

        for (int i = 0; i < 20; i++) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            writeImage(resized, baos, quality, format);
            int sizeKB = baos.size() / 1024;
            lastValidBytes = baos.toByteArray();

            if (sizeKB <= targetKB || quality < 0.1f) {
                try (FileOutputStream fos = new FileOutputStream(outFile)) {
                    fos.write(baos.toByteArray());
                }
                break;
            }
            quality -= 0.05f;
        }

        if (!outFile.exists() || outFile.length() == 0) {
            try (FileOutputStream fos = new FileOutputStream(outFile)) {
                fos.write(lastValidBytes);
            }
        }

        ImageInfo info = new ImageInfo();
        info.setFileName(originalFile.getName());
        info.setResizedFilePath(outFile.getAbsolutePath());
        info.setOriginalWidth(original.getWidth());
        info.setOriginalHeight(original.getHeight());
        info.setOriginalSizeKB(originalKB);
        info.setNewWidth(newW);
        info.setNewHeight(newH);
        info.setNewSizeKB(outFile.length() / 1024);
        info.setResizedFileName(outFile.getName());
        return info;
    }


    private void writeImage(BufferedImage img, OutputStream os, float quality, String format) throws IOException {
        if (format.equalsIgnoreCase("jpg") || format.equalsIgnoreCase("jpeg")) {
            BufferedImage rgbImage = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D g = rgbImage.createGraphics();
            g.drawImage(img, 0, 0, Color.WHITE, null);
            g.dispose();

            Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
            if (!writers.hasNext()) throw new IOException("No JPEG writer found");
            ImageWriter writer = writers.next();

            ImageWriteParam param = writer.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(quality);

            try (ImageOutputStream ios = ImageIO.createImageOutputStream(os)) {
                writer.setOutput(ios);
                writer.write(null, new IIOImage(rgbImage, null, null), param);
            } finally {
                writer.dispose();
            }
        } else {
            ImageIO.write(img, format, os);
        }
    }
}
