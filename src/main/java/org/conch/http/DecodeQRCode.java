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
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import org.conch.ConchException;
import org.conch.util.Convert;
import org.conch.util.Logger;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>The DecodeQRCode API converts a base64-encoded image of a
 * 2-D QR (Quick Response) code to a UTF-8 string, using the ZXing library.
 * </p>
 * 
 * <p>The input qrCodeBase64 can be the output of the DecodeQRCode API.</p>
 * 
 * <p>Request parameters:</p>
 * 
 * <ul>
 * <li>qrCodeBase64 - A base64 string encoded from an image of a QR code.
 * The length of the string must be less than the jetty server maximum allowed
 * parameter length, currently 200,000 bytes.
 * </li>
 * </ul>
 * 
 * <p>Response fields:</p>
 * 
 * <ul>
 * <li>qrCodeData - A UTF-8 string decoded from the QR code.</li>
 * </ul>
 */

public final class DecodeQRCode extends APIServlet.APIRequestHandler {

    static final DecodeQRCode instance = new DecodeQRCode();

    private DecodeQRCode() {
        super(new APITag[] {APITag.UTILS}, "qrCodeBase64");
    }
    
    @Override
    protected JSONStreamAware processRequest(HttpServletRequest request)
            throws ConchException {
   
        String qrCodeBase64 = Convert.nullToEmpty(request.getParameter("qrCodeBase64"));

        JSONObject response = new JSONObject();
        try {
            BinaryBitmap binaryBitmap = new BinaryBitmap(
                    new HybridBinarizer(new BufferedImageLuminanceSource(
                            ImageIO.read(new ByteArrayInputStream(
                                    Base64.getDecoder().decode(qrCodeBase64)
                            ))
                    ))
            );

            Map hints = new HashMap();
            hints.put(DecodeHintType.POSSIBLE_FORMATS, EnumSet.of(BarcodeFormat.QR_CODE));
            hints.put(DecodeHintType.CHARACTER_SET, "UTF-8");
            
            Result qrCodeData = new MultiFormatReader().decode(binaryBitmap, hints);
            response.put("qrCodeData", qrCodeData.getText());
        } catch(IOException ex) {
            String errorMessage = "Error reading base64 byte stream";
            Logger.logErrorMessage(errorMessage, ex);
            JSONData.putException(response, ex, errorMessage);
        } catch(NullPointerException ex) {
            String errorMessage = "Invalid base64 image";
            Logger.logErrorMessage(errorMessage, ex);
            JSONData.putException(response, ex, errorMessage);
        } catch(NotFoundException ex) {
            response.put("qrCodeData", "");
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
