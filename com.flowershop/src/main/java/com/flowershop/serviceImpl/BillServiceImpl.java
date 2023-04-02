package com.flowershop.serviceImpl;

import com.flowershop.JWT.JwtFilter;
import com.flowershop.POJO.Bill;
import com.flowershop.constants.FlowerShopConstants;
import com.flowershop.dao.BillDao;
import com.flowershop.service.BillService;
import com.flowershop.utils.FlowerShopUtils;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.io.IOUtils;
import org.json.JSONArray;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static com.itextpdf.text.FontFactory.*;

@Slf4j
@Service
public class BillServiceImpl implements BillService {

    @Autowired
    JwtFilter jwtFilter;

    @Autowired
    BillDao billDao;

    @Override
    public ResponseEntity<String> generateReport(Map<String, Object> requestMap) {
        log.info("Inside generateReport");
        try {
            String fileName;
            if (validateRequestMap(requestMap)) {
                if (requestMap.containsKey("isGenerate") && !(Boolean)requestMap.get("isGenerate")) {
                    fileName = (String) requestMap.get("uuid");
                }
                else {
                    fileName = FlowerShopUtils.getUUID();
                    requestMap.put("uuid", fileName);
                    insertBill(requestMap);
                }

                String data = "\n\nName: " + requestMap.get("name") + "\nPhone number:" + requestMap.get("phoneNr") +
                        "\nEmail: " + requestMap.get("email") + "\nPayment Method: " + requestMap.get("paymentMethod");

                Document document = new Document();
                PdfWriter.getInstance(document, new FileOutputStream(FlowerShopConstants.PDF_LOCATION + "\\" + fileName + ".pdf" ));

                document.open();
                setRectangleInPdf(document);

                Paragraph title = new Paragraph("Tulipa Flower Shop", getFont(HELVETICA_BOLD, 18, BaseColor.BLACK));
                title.setAlignment(Element.ALIGN_CENTER);
                document.add(title);

                Paragraph userData = new Paragraph(data + "\n\n", getFont(TIMES_ROMAN, 12, BaseColor.BLACK));
                document.add(userData);

                PdfPTable table = new PdfPTable(5);
                table.setWidthPercentage(100);
                addTableHeader(table);

                JSONArray jsonArray = FlowerShopUtils.getJsonArrayFromString((String) requestMap.get("productDetails"));
                for(int i = 0; i < jsonArray.length(); i++) {
                    addRows(table, FlowerShopUtils.getMapFromJson(jsonArray.getString(i)));
                }
                document.add(table);

                Paragraph total = new Paragraph("Total: " + requestMap.get("totalAmount") + " RON", getFont(TIMES_BOLD, 13, BaseColor.BLACK));
                total.setAlignment(Element.ALIGN_RIGHT);
                document.add(total);
                document.close();
                return new ResponseEntity<>("{\"uuid\":\""+ fileName + "\"}", HttpStatus.OK);

            }
            else {
                return FlowerShopUtils.getResponseEntity(FlowerShopConstants.INVALID_DATA, HttpStatus.BAD_REQUEST);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return FlowerShopUtils.getResponseEntity(FlowerShopConstants.SOMETHING_WENT_WRONG, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private void addRows(PdfPTable table, Map<String, Object> mapFromJson) {
        table.addCell((String) mapFromJson.get("name"));
        table.addCell((String) mapFromJson.get("category"));
        table.addCell((String) mapFromJson.get("quantity"));
        table.addCell(Double.toString((Double)mapFromJson.get("price")));
        table.addCell(Double.toString((Double)mapFromJson.get("total")));
    }

    private void addTableHeader(PdfPTable table) {
        Stream.of("Name", "Category", "Quantity", "Price", "Subtotal")
                .forEach(columnTitle ->
        {
            PdfPCell header = new PdfPCell();
            header.setBackgroundColor(BaseColor.BLACK);
            header.setBorderWidth(2);
            header.setPhrase(new Phrase(columnTitle));
            header.setBackgroundColor(BaseColor.LIGHT_GRAY);
            header.setHorizontalAlignment(Element.ALIGN_CENTER);
            header.setVerticalAlignment(Element.ALIGN_CENTER);
            table.addCell(header);

        });
    }

    private void setRectangleInPdf(Document document) throws DocumentException {
        Rectangle rect = new Rectangle(577, 825, 18, 15);
        rect.enableBorderSide(1);
        rect.enableBorderSide(2);
        rect.enableBorderSide(4);
        rect.enableBorderSide(8);
        rect.setBorderColor(BaseColor.BLACK);
        rect.setBorderWidth(1);
        document.add(rect);
    }

    private boolean validateRequestMap(Map<String, Object> requestMap) {
        return requestMap.containsKey("name") &&
                requestMap.containsKey("phoneNr") &&
                requestMap.containsKey("email") &&
                requestMap.containsKey("paymentMethod") &&
                requestMap.containsKey("productDetails") &&
                requestMap.containsKey("totalAmount");
    }

    private void insertBill(Map<String, Object> requestMap) {
        try {
            Bill bill = new Bill();
            bill.setUuid((String) requestMap.get("uuid"));
            bill.setName((String) requestMap.get("name"));
            bill.setEmail((String) requestMap.get("email"));
            bill.setPhoneNr((String) requestMap.get("phoneNr"));
            bill.setPaymentMethod((String) requestMap.get("paymentMethod"));
            bill.setTotal(Integer.parseInt((String) requestMap.get("totalAmount")));
            bill.setProductDetail((String) requestMap.get("productDetail"));
            bill.setCreatedBy(jwtFilter.getCurrentUser());
            billDao.save(bill);

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public ResponseEntity<List<Bill>> getBills() {
        try {
            List<Bill> list = new ArrayList<>();
            if(jwtFilter.isAdmin()) {
                list = billDao.getAllBills();
            }
            else {
                list = billDao.getBillByUserName(jwtFilter.getCurrentUser());
            }
            return new ResponseEntity<>(list, HttpStatus.OK);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return new ResponseEntity<>(new ArrayList<>(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Override
    public ResponseEntity<byte[]> getPdf(Map<String, Object> requestMap) {
        try {
            byte[] byteArray = new byte[0];
            if(!requestMap.containsKey("uuid") && validateRequestMap(requestMap)) {
                return new ResponseEntity<>(byteArray, HttpStatus.BAD_REQUEST);
            }
            String filePath = FlowerShopConstants.PDF_LOCATION + "\\" + (String) requestMap.get("uuid") + ".pdf";
            if(FlowerShopUtils.doesFileExist(filePath)) {
                byteArray = getByteArray(filePath);
                return new ResponseEntity<>(byteArray, HttpStatus.OK);
            }
            else {
                requestMap.put("isGenerate", false);
                generateReport(requestMap);
                byteArray = getByteArray(filePath);
                return new ResponseEntity<>(byteArray,HttpStatus.OK);
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    private byte[] getByteArray(String filePath) throws Exception{
        File file = new File(filePath);
        InputStream targetStream = new FileInputStream(file);
        byte[] byteArray = IOUtils.toByteArray(targetStream);
        targetStream.close();
        return byteArray;
    }

    @Override
    public ResponseEntity<String> deleteBill(Integer id) {
        try {
            Optional optional = billDao.findById(id);
            if(!optional.isEmpty()) {
                billDao.deleteById(id);
                return FlowerShopUtils.getResponseEntity("Bill deleted successfully!", HttpStatus.OK);
            }
            return FlowerShopUtils.getResponseEntity("Bill id does not exist!", HttpStatus.OK);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return FlowerShopUtils.getResponseEntity(FlowerShopConstants.SOMETHING_WENT_WRONG, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
