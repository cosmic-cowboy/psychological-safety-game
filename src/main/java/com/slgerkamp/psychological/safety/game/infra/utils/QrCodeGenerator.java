package com.slgerkamp.psychological.safety.game.infra.utils;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.springframework.stereotype.Component;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Map;

@Component
public class QrCodeGenerator {

    public void create(String url, String stageId) {
        final int size = 150;
        final String filePath = createPath(stageId);

        QRCodeWriter writer = new QRCodeWriter();
        Path path = FileSystems.getDefault().getPath(filePath);

        try {
            Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
            hints.put(EncodeHintType.MARGIN, 0);
            BitMatrix bitMatrix = writer.encode(url, BarcodeFormat.QR_CODE, size, size, hints);
            MatrixToImageWriter.writeToPath(bitMatrix, "PNG", path);
        } catch (WriterException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    public InputStream read(String stageId) {
        InputStream is;
        final String filePath = createPath(stageId);
        try {
            is = new FileInputStream(filePath);
        } catch (FileNotFoundException e) {
            final String url = CommonUtils.createStageUrl(stageId);
            create(url, stageId);
            is = read(stageId);
        }
        return is;
    }

    private String createPath(String stageId) {
        return "qr_code-" + stageId + ".png";
    }

}

