/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.service.domain;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import javax.imageio.ImageIO;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import net.schwehla.matrosdms.domain.content.MDocumentStream;
import net.schwehla.matrosdms.store.IMatrosStore;

@Service
public class ThumbnailService {

	private static final Logger log = LoggerFactory.getLogger(ThumbnailService.class);

	@Autowired
	IMatrosStore store;

	public byte[] getThumbnail(String uuid) {
		// 1. Try Cache
		if (store.hasThumbnail(uuid)) {
			return store.loadThumbnail(uuid);
		}

		// 2. Generate
		try {
			log.info("Generating thumbnail for {}", uuid);
			MDocumentStream content = store.loadStream(uuid);
			if (content == null)
				return null;

			byte[] imageBytes = null;
			String fn = content.getFilename().toLowerCase();

			try (InputStream is = content.getInputStream()) {
				if (fn.endsWith(".pdf")) {
					try (PDDocument doc = Loader.loadPDF(is.readAllBytes())) {
						PDFRenderer renderer = new PDFRenderer(doc);
						// Scale: 72 DPI is roughly screen res, 0.5 scale for thumb
						BufferedImage image = renderer.renderImage(0, 0.5f);
						imageBytes = toJpeg(image);
					}
				} else if (fn.endsWith(".jpg") || fn.endsWith(".png") || fn.endsWith(".jpeg")) {
					BufferedImage original = ImageIO.read(is);
					if (original != null) {
						// Create a small version (max 300px width)
						int w = original.getWidth();
						int h = original.getHeight();
						double ratio = 300.0 / w;
						if (ratio < 1.0) {
							int newW = (int) (w * ratio);
							int newH = (int) (h * ratio);
							java.awt.Image scaled = original.getScaledInstance(newW, newH, java.awt.Image.SCALE_SMOOTH);
							BufferedImage thumb = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_RGB);
							thumb.getGraphics().drawImage(scaled, 0, 0, null);
							imageBytes = toJpeg(thumb);
						} else {
							// Already small
							imageBytes = toJpeg(original);
						}
					}
				}
			}

			// 3. Cache & Return
			if (imageBytes != null) {
				store.storeThumbnail(uuid, imageBytes);
				return imageBytes;
			}

		} catch (Exception e) {
			log.error("Thumb generation failed for " + uuid, e);
		}

		return null; // Frontend handles 404
	}

	private byte[] toJpeg(BufferedImage image) throws Exception {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ImageIO.write(image, "jpg", baos);
		return baos.toByteArray();
	}
}