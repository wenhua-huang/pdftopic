package com.readexcel.demo;

import com.baidu.aip.ocr.AipOcr;
import org.apache.pdfbox.io.IOUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.poi.ss.usermodel.Workbook;
import org.jeecgframework.poi.excel.ExcelExportUtil;
import org.jeecgframework.poi.excel.entity.ExportParams;
import org.jeecgframework.poi.excel.entity.params.ExcelExportEntity;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import java.util.List;


public class readPdf2Excel {
	//调用百度文字识别 api，所需的参数
	public static final String APP_ID = "14848114";
	public static final String API_KEY = "KTgYKIdiSydVEi5jWMGsUMY0";
	public static final String SECRET_KEY = "d51jfFTptl3pym7oULZR0zzNudtuC1Lc";
	//excle表头
	private final static  List<ExcelExportEntity> excelExportEntities=Arrays.asList(
			new ExcelExportEntity("开票抬头","purchaserName",15),
			new ExcelExportEntity("发票号码","invoiceCode",15),
			new ExcelExportEntity("开票日期","invoiceDate",15),
			new ExcelExportEntity("金额","totalAmount",15),
			new ExcelExportEntity("税率","totalTaxRateStr",15),
			new ExcelExportEntity("税额","totalTax",15),
			new ExcelExportEntity("税价合计","amountInFiguers",15)
	);

	/**
	 * 读取发票内容 生成excle表格
	 * @param path 目录
	 * @throws IOException
	 */
	public static void pdf2Excel(String path) throws IOException {
		ArrayList<String> pdfs = getPdfFilePath(path, "pdf");
		List<Map<String,Object>> rowMapList=new ArrayList<>();
		for(String pdf:pdfs){
			List<BufferedImage> images=	readPdf(pdf);

			for(BufferedImage bufferedImage:images){

				ByteArrayOutputStream os = new ByteArrayOutputStream();
				ImageIO.write(bufferedImage, "png", os);
				byte[] imageBytes = os.toByteArray();
				//writeImageToDisk(imageBytes);
				JSONObject vatInvoiceRecognition = vatInvoiceRecognition(imageBytes);
				HashMap<String,Object> rowMap=jObjToMap(vatInvoiceRecognition);
				rowMapList.add(rowMap);
			}
		}
		createExcelByMap(rowMapList,excelExportEntities);
	}

	/**
	 * 百度返回的json 中获取我们需要的字段 组成map 返回
	 * @param vatInvoiceRecognition
	 * @return
	 */
	private static HashMap<String,Object> jObjToMap(JSONObject vatInvoiceRecognition) {
		JSONObject wordsResult = (JSONObject)vatInvoiceRecognition.get("words_result");
		String purchaserName =(String) wordsResult.get("PurchaserName");//购买方 抬头
		String invoiceCode =(String) wordsResult.get("InvoiceCode");//发票号码
		String invoiceDate =(String) wordsResult.get("InvoiceDate");//发票日期
		String totalAmount =(String) wordsResult.get("TotalAmount");//合计金额
		String totalTax=(String) wordsResult.get("TotalTax");//合计税额
		String amountInFiguers=(String)wordsResult.get("AmountInFiguers");//价税合计(小写)
	/*	ArrayList<JSONObject> commodityAmountArr = (ArrayList<JSONObject>)wordsResult.get("CommodityAmount");
		for(JSONObject commodityAmountObj:  commodityAmountArr){
			String commodityAmount=(String) commodityAmountObj.get("word");//现在只有一行每一行的 金额
		}*/
		String commodityTax=null;
		JSONArray commodityTaxArr = (JSONArray)wordsResult.get("CommodityTax");
		for(Object commodityTaxObj:  commodityTaxArr){
			JSONObject commodityTaxJsonObj=(JSONObject)commodityTaxObj;
			commodityTax=(String) commodityTaxJsonObj.get("word");//现在只有一行 每一行的 税额
		}
		String commodityTaxRate=null;
		JSONArray commodityTaxRateArr =(JSONArray)wordsResult.get("CommodityTaxRate");
		for(Object  commodityTaxRateObj:  commodityTaxRateArr){
			JSONObject commodityTaxRateSONObj= (JSONObject)commodityTaxRateObj;
			commodityTaxRate=(String) commodityTaxRateSONObj.get("word");//现在只有一行 每一行的 税率
		}
		//BigDecimal totalTaxRate=totalTax.divide(amountInFiguers,4, BigDecimal.ROUND_HALF_UP);
		//String totalTaxRateStr=totalTaxRate.multiply(new BigDecimal("100")).toString()+"%";
		HashMap<String, Object> rowMap = new HashMap<>();
		rowMap.put("purchaserName",purchaserName);
		rowMap.put("invoiceCode",invoiceCode);
		rowMap.put("invoiceDate",invoiceDate);
		rowMap.put("totalAmount",totalAmount);//合计金额
		rowMap.put("totalTaxRateStr",commodityTaxRate);//税率
		rowMap.put("totalTax",commodityTax);//合计税额
		rowMap.put("amountInFiguers",amountInFiguers);//价税合计(小写)


		return rowMap;
	}


	/**
	 * pdf 转图片
	 * @param filePath  pdf 文件的绝对路径
	 * @return   List<BufferedImage>
	 * @throws IOException
	 */
	public static List<BufferedImage> readPdf(String filePath) throws IOException {
		PDDocument document = PDDocument.load(new File(filePath));
		PDFRenderer pdfRenderer = new PDFRenderer(document);
		List<BufferedImage> bufferedImageList = new ArrayList<>();
		for (int page = 0;page<document.getNumberOfPages();page++){
			BufferedImage img = pdfRenderer.renderImageWithDPI(page, 300, ImageType.RGB);
			bufferedImageList.add(img);
		}
		document.close();
		return bufferedImageList;
	}
	public static BufferedImage concat(BufferedImage[] images) throws IOException {
		int heightTotal = 0;
		for(int j = 0; j < images.length; j++) {
			heightTotal += images[j].getHeight();
		}
		int heightCurr = 0;
		BufferedImage concatImage = new BufferedImage(images[0].getWidth(), heightTotal, BufferedImage.TYPE_INT_RGB);
		Graphics2D g2d = concatImage.createGraphics();
		for(int j = 0; j < images.length; j++) {
			g2d.drawImage(images[j], 0, heightCurr, null);
			heightCurr += images[j].getHeight();
		}
		g2d.dispose();
		return concatImage;
	}


	/**
	 * 获取文件的绝对路径
	 * @param path 目录
	 * @param endName pdf
	 * @return
	 */
	public static ArrayList<String> getPdfFilePath(String path, String endName){
		File file = new File(path);
		ArrayList<String> pdfPaths = new ArrayList<>();
		if(file.exists()){
			File[] files = file.listFiles();
			Arrays.stream(files).filter(pdfFile->
				 pdfFile.getAbsolutePath().endsWith("pdf")).forEach(pdf->
					pdfPaths.add(pdf.getAbsolutePath())
			);
		}
		return pdfPaths;
	}

	/**
	 * 通用文字识别
	 * @param inputStream
	 * @return
	 */
	public static String getContainerInfo(InputStream inputStream) {
		// 初始化一个AipOcr
		AipOcr client = new AipOcr(APP_ID, API_KEY, SECRET_KEY);
		// 可选：设置网络连接参数
		client.setConnectionTimeoutInMillis(2000);
		client.setSocketTimeoutInMillis(60000);
		// 调用接口
		byte[] image = null;
		try {
			image  = IOUtils.toByteArray(inputStream);
			System.out.print("图片");
			for(byte b : image){
				System.out.print(b);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		JSONObject res = client.basicGeneral(image, new HashMap<String, String>());
		String result = null;
		try {
			System.out.println(res.toString(2));
			result = res.toString(2);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return result;
	}
	/**
	 * 将图片写入到磁盘
	 * @param img 图片数据流
	 * @param
	 */
	public static void writeImageToDisk(byte[] img){
		try {
			File file = new File("D:\\yazuishou\\doc\\vatInvoiceRec\\picture\\" + +System.currentTimeMillis()+".png");
			FileOutputStream fops = new FileOutputStream(file);
			fops.write(img);
			fops.flush();
			fops.close();
			System.out.println("图片已经写入到D盘");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	/**
	 * 增值税发票识别   VAT invoice recognition
	 * @param  image 图片流
	 */
	public static JSONObject vatInvoiceRecognition(byte[] image) throws IOException {
		// 初始化一个AipOcr
		AipOcr client = new AipOcr(APP_ID, API_KEY, SECRET_KEY);
		// 可选：设置网络连接参数
		client.setConnectionTimeoutInMillis(2000);
		client.setSocketTimeoutInMillis(60000);
		// 调用接口
		return  client.vatInvoice(image, new HashMap<String, String>());
	}

	/**
	 * 根据传入的List<Map<String,Object>生成excel文件
	 * @param list  表头
	 *
	 */
	public static void createExcelByMap(List<Map<String,Object>> list,List<ExcelExportEntity> entitys){
		try {
			Workbook wb = ExcelExportUtil.exportExcel(new ExportParams("发票", "sheet"), entitys, list);
			String fileName = "D:\\"+System.currentTimeMillis()+".xls";
			FileOutputStream fout = new FileOutputStream(fileName);
			wb.write(fout);
			fout.close();
			wb.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
