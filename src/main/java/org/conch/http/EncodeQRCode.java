/*
 *  Copyright © 2017-2018 Sharder Foundation.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License
 *  version 2 as published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, you can visit it at:
 *  https://www.gnu.org/licenses/old-licenses/gpl-2.0.txt
 *
 *  This software uses third party libraries and open-source programs,
 *  distributed under licenses described in 3RD-PARTY-LICENSES.
 *
 */

package org.conch.http;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import org.conch.common.ConchException;
import org.conch.util.Convert;
import org.conch.util.Logger;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>The EncodeQRCode API converts a UTF-8 string to a base64-encoded
 * jpeg image of a 2-D QR (Quick Response) code, using the ZXing library.</p>
 * 
 * <p>The output qrCodeBase64 string can be incorporated into an in-line HTML
 * image like this: &lt;img src="data:image/jpeg;base64,qrCodeBase64"&gt;
 * </p>
 * 
 * <p>The output qrCodeBase64 can be input to the DecodeQRCode API to
 * recover the original qrCodeData.</p>
 * 
 * <p>Request parameters:</p>
 * 
 * <ul>
 * <li>qrCodeData - A UTF-8 text string.</li>
 * <li>width - The width of the output image in pixels, optional.</li>
 * <li>height - The height of the output image in pixels, optional.</li>
 * </ul>
 * 
 * <p>Notes:</p>
 * <ul>
 * <li>The output image consists of a centrally positioned square QR code
 * with a size which is an integer multiple of the minimum size, surrounded by 
 * sufficient white padding to achieve the requested width/height.</li>
 * <li>The default width/height of 0 results in the minimum sized output
 * image, with one pixel per black/white region of the QR code and no
 * extra padding.</li>
 * <li>To eliminate padding, the requested width/height must be an integer
 * multiple of the minimum.</li>
 * </ul>
 * 
 * <p>Response fields:</p>
 * 
 * <ul>
 * <li>qrCodeBase64 - A base64 string encoding a jpeg image of
 * the QR code.</li>
 * </ul>
 */

public final class EncodeQRCode extends APIServlet.APIRequestHandler {

    static final EncodeQRCode instance = new EncodeQRCode();

    private EncodeQRCode() {
        super(new APITag[] {APITag.UTILS}, "qrCodeData", "width", "height");
    }
    
    @Override
    protected JSONStreamAware processRequest(HttpServletRequest request)
            throws ConchException {
        
        JSONObject response = new JSONObject();

        String qrCodeData = Convert.nullToEmpty(request.getParameter("qrCodeData"));

        int width = ParameterParser.getInt(request, "width", 0, 5000, false);
        int height = ParameterParser.getInt(request, "height", 0, 5000, false);
        
        try {
            Map hints = new HashMap();
            // Error correction level: L (7%), M (15%), Q (25%), H (30%) -- Default L.
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
            hints.put(EncodeHintType.MARGIN, 0); // Default 4
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");

            BitMatrix matrix = new MultiFormatWriter().encode(qrCodeData,
                    BarcodeFormat.QR_CODE,
                    width, 
                    height, 
                    hints
            );
            BufferedImage bufferedImage = MatrixToImageWriter.toBufferedImage(matrix);
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            ImageIO.write(bufferedImage, "jpeg", os);
            byte[] bytes = os.toByteArray();
            os.close();
            String base64 = Base64.getEncoder().encodeToString(bytes);
            response.put("qrCodeBase64", base64);
        } catch(WriterException|IOException ex) {
            String errorMessage = "Error creating image from qrCodeData";
            Logger.logErrorMessage(errorMessage, ex);
            JSONData.putException(response, ex, errorMessage);
        }
        return response;
    }

    @Override
    protected final boolean requirePost() {
        return true;
    }

    @Override
    protected boolean allowRequiredBlockParameters() {
        return false;
    }

    @Override
    protected boolean requireBlockchain() {
        return false;
    }

}
