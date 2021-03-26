package org.conch.http;

import org.apache.poi.hssf.usermodel.*;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.util.CellRangeAddress;
import org.conch.Conch;
import org.conch.account.Account;
import org.conch.chain.Block;
import org.conch.chain.BlockImpl;
import org.conch.common.ConchException;
import org.conch.db.Db;
import org.conch.db.DbIterator;
import org.conch.db.DbUtils;
import org.conch.mint.MintStatisticsData;
import org.conch.util.Convert;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Manager can use this API to obtain all of miner mining data
 * <p>
 * Parameters
 * <ul>
 * <li>startHeight - height from
 * <li>endHeight - height to
 * </ul>
 */
public class GetMinerStatistics extends APIServlet.APIRequestHandler {

    static final GetMinerStatistics instance = new GetMinerStatistics();

    private GetMinerStatistics() {
        super(new APITag[]{APITag.INFO}, "startHeight", "endHeight");
    }

    /**
     * http request return the json format of miner mining data
     *
     * @param request
     * @return
     * @throws ConchException
     */
    @Override
    protected JSONStreamAware processRequest(HttpServletRequest request) throws ConchException {
        String startHeightValue = Convert.emptyToNull(request.getParameter("startHeight"));
        String endHeightValue = Convert.emptyToNull(request.getParameter("endHeight"));
        int startHeight = 0;
        int endHeight = 0;
        if (startHeightValue != null) {
            startHeight = Integer.parseInt(startHeightValue);
        }
        if (endHeightValue != null) {
            endHeight = Integer.parseInt(endHeightValue);
        }

        DbIterator<BlockImpl> blocks = null;
        Map<Long, MintStatisticsData> data = new HashMap<>();
        try {
            blocks = Conch.getBlockchain().getBlocksByHeight(startHeight, endHeight, new String[]{"TIMESTAMP", "ASC"});
            for (Block block : blocks) {
                if (data.containsKey(block.getGeneratorId())) {
                    data.get(block.getGeneratorId()).updateData(block, startHeight, endHeight);
                } else {
                    data.put(block.getGeneratorId(), MintStatisticsData.init(block.getGeneratorId(),
                            block.getTimestamp(), startHeight, endHeight));
                }
            }
        } finally {
            DbUtils.close(blocks);
        }
        //downloadData(data);
        return JSONData.minerStatistics(data);
    }

    /**
     * download the excel of miner mining data to local
     *
     * @param data
     */
    private static void downloadData(Map<Long, MintStatisticsData> data, String savaPath, int startHeight,
                                     int endHeight) {
        HSSFWorkbook wb = new HSSFWorkbook();
        HSSFSheet sheet = wb.createSheet("矿工信息统计");
        String[] titles = {"账号", "出块总数", "出块比率", "最近一次出块时间", "平均出块时间(天)",
                "矿机ip", "节点类型", "pocScore", "pocDetail"};
        int rowNum = 0;
        HSSFRow row = sheet.createRow(rowNum);
        HSSFCellStyle style = wb.createCellStyle();
        style.setAlignment(HorizontalAlignment.CENTER);
        creatCell(row, style, new String[titles.length]);
        HSSFCell baseMsg = row.getCell(0);
        HSSFCellStyle leftStyle = wb.createCellStyle();
        leftStyle.setAlignment(HorizontalAlignment.LEFT);
        baseMsg.setCellStyle(leftStyle);
        baseMsg.setCellValue("起始高度：" + startHeight + "，" + "截止高度：" + endHeight);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, titles.length - 1));


        row = sheet.createRow(++rowNum);
        creatCell(row, style, titles);

        for (Long key : data.keySet()) {
            MintStatisticsData mintStatisticsData = data.get(key);
            row = sheet.createRow(++rowNum);
            creatCell(row, style, new String[]{Account.rsAccount(key),
                    mintStatisticsData.getGenerateCount().toString(),
                    mintStatisticsData.getGenerateRate().multiply(new BigDecimal("100")).toString() + "%",
                    Convert.dateFromEpochTime(mintStatisticsData.getLatestMiningTime()),
                    mintStatisticsData.getAvgMiningTime() == null ? "--" :
                            new BigDecimal(mintStatisticsData.getAvgMiningTime() * 1000)
                                    .divide(new BigDecimal(24 * 60 * 60 * 1000), 2, BigDecimal.ROUND_FLOOR)
                                    .toString(),
                    mintStatisticsData.getMiningMachineIP(),
                    mintStatisticsData.getNoteType(),
                    mintStatisticsData.getPocScore().getTotal().toString(),
                    JSONData.phasingPocScore(mintStatisticsData.getPocScore())
            });
        }

        try {
            String fileName = "MinerStatistics-" + new SimpleDateFormat("yyyyMMddhhmmss").format(new Date()) + ".xls";
            FileOutputStream fos = new FileOutputStream(savaPath + "\\" + fileName);
            wb.write(fos);
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * creat cells of row
     *
     * @param row
     * @param style
     * @param contents
     */
    private static void creatCell(HSSFRow row, HSSFCellStyle style, String[] contents) {
        HSSFCell cell = row.createCell((short) 0);
        for (int i = 0; i < contents.length; i++) {
            cell.setCellValue(contents[i]);
            cell.setCellStyle(style);
            cell = row.createCell((short) i + 1);
        }
    }

    /**
     * case of download, use it to download a excel, modify savePath with your local path
     *
     * @param args
     */
    public static void main(String[] args) {
        Db.init();
        int startHeight = 1;
        int endHeight = 4000;

        DbIterator<BlockImpl> blocks = null;
        Map<Long, MintStatisticsData> data = new HashMap<>();
        try {
            blocks = Conch.getBlockchain().getBlocksByHeight(startHeight, endHeight, new String[]{"TIMESTAMP", "ASC"});
            for (Block block : blocks) {
                if (data.containsKey(block.getGeneratorId())) {
                    data.get(block.getGeneratorId()).updateData(block, startHeight, endHeight);
                } else {
                    data.put(block.getGeneratorId(), MintStatisticsData.init(block.getGeneratorId(),
                            block.getTimestamp(), startHeight, endHeight));
                }
            }
        } finally {
            DbUtils.close(blocks);
        }
        downloadData(data, "D:\\data", startHeight, endHeight);
    }
}
