package com.example.unit_testing_automation.Service;

import com.example.unit_testing_automation.Model.TestFile;
import com.example.unit_testing_automation.Repository.FileUploadRepository;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.awt.*;
import java.awt.Color;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class FileUploadService {

    @Autowired
    private FileUploadRepository repository;


    public ResponseEntity<String> uploadModule(MultipartFile file) throws IOException {
        List<String> methodsList = new ArrayList<>();

        String originalUploadedFileName = file.getOriginalFilename();
        Path tempDir = Files.createTempDirectory("compile_dir");
        System.out.println("Temp File Directory: "+tempDir);
        File tempJavaFile = new File(tempDir.toFile(), originalUploadedFileName);
        file.transferTo(tempJavaFile);
        tempJavaFile.deleteOnExit();

        String result = executeTests(tempJavaFile,originalUploadedFileName);
        return new ResponseEntity<>(result,HttpStatus.OK);
    }

    private List<File> getJavaFilesInFolder(File folder) {
        List<File> javaFiles = new ArrayList<>();
        if (folder.isDirectory()) {
            File[] files = folder.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        // Recursively get Java files in subfolders
                        javaFiles.addAll(getJavaFilesInFolder(file));
                    } else if (file.getName().endsWith(".java")) {
                        javaFiles.add(file);
                    }
                }
            }
        }
        return javaFiles;
    }

    public void extractZipFile(MultipartFile zipFile, Path destination) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(zipFile.getInputStream())) {
            ZipEntry zipEntry;
            while ((zipEntry = zis.getNextEntry()) != null) {
                Path newFilePath = destination.resolve(zipEntry.getName()); // Path for the extracted file
                if (zipEntry.isDirectory()) {
                    Files.createDirectories(newFilePath); // If it's a folder, create folder
                } else {
                    // If it's a file, create the file and write the content
                    try (BufferedOutputStream bos = new BufferedOutputStream(Files.newOutputStream(newFilePath))) {
                        byte[] buffer = new byte[1024];
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            bos.write(buffer, 0, len); // If it's a file, save content
                        }
                    }
                }
                zis.closeEntry(); // Move to the next entry in the zip
            }
        }
    }


    public String executeTests(File tempJavaFile, String fileName){
        try{
            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            int compiledResult = compiler.run(null,null,null, tempJavaFile.getPath());
            File tempParentDir = tempJavaFile.getParentFile();
            if(compiledResult!=0)
                return "Error occurred while compiling java class file";

            String javaFileName = fileName.replace(".java","");
            String className = "com.example.unit_testing_automation.TestDataFile."+javaFileName;

            URLClassLoader classLoader= URLClassLoader.newInstance(new URL[]{tempParentDir.toURI().toURL()});
            Class<?> clazz = Class.forName(className,true,classLoader);
            Object instance = clazz.getDeclaredConstructor().newInstance();

//            Method methodRetrieved = clazz.getDeclaredMethod("addition");
//            System.out.println("The Method name is: "+methodRetrieved.getName());

            for(Method method: clazz.getDeclaredMethods()){
                if(method.getParameterCount()==0){
                    method.setAccessible(true);
                    Object methodResult =  method.invoke(instance);
                    String actualOutput = (methodResult!=null)? methodResult.toString() : "null";
                    String expectedOutput = getMethodExpectedOutput(method.getName());
                    String testStatus = (actualOutput.equals(expectedOutput))? "Passed" : "Failed";
                    String methodName = method.getName();
                    TestFile testFile = new TestFile(javaFileName,methodName,testStatus);
                    repository.save(testFile);
                }
            }
        } catch (Exception e) {
            System.out.println("Error occurred while running tests: "+e.getMessage());
        }
        return "File uploaded and tested successfully";
    }

    private String getMethodExpectedOutput(String methodName) {
        String expectedOutput = switch (methodName) {
            case "testingSimulation" -> "method testingSimulation tested successfully";
            case "addition" -> "Answer is: 20";
            case "multiplication" -> "Answer is: 110";
            default -> "";
        };
        return expectedOutput;
    }


    // Get All
    public List<TestFile> allTestReports() {
        List<TestFile> testFileList = repository.findAll().stream()
                .sorted(Comparator.comparingInt(TestFile::getId))
                .toList();;
        return testFileList;
    }


    //excel generator
    public void exportToExcel(HttpServletResponse response) throws IOException {
        List<TestFile> testFile = repository.findAll().stream()
                .sorted(Comparator.comparingInt(TestFile::getId))
                .toList();

        XSSFWorkbook workbook = new XSSFWorkbook();
        XSSFSheet sheet = workbook.createSheet("JavaFile test report");

        // Create styles
        XSSFCellStyle headerStyle = workbook.createCellStyle();
        XSSFFont headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setFontHeightInPoints((short) 12);
        XSSFFont normalFont = workbook.createFont();
        normalFont.setFontHeightInPoints((short) 12);

        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(customizedColumnColor(120, 162, 222));
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        headerStyle.setBorderTop(BorderStyle.THIN);
        headerStyle.setBorderBottom(BorderStyle.THIN);
        headerStyle.setBorderLeft(BorderStyle.THIN);
        headerStyle.setBorderRight(BorderStyle.THIN);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);

        CellStyle borderStyle = workbook.createCellStyle();
        borderStyle.setAlignment(HorizontalAlignment.CENTER);
        borderStyle.setVerticalAlignment(VerticalAlignment.CENTER);

        borderStyle.setBorderTop(BorderStyle.THIN);
        borderStyle.setBorderBottom(BorderStyle.THIN);
        borderStyle.setBorderLeft(BorderStyle.THIN);
        borderStyle.setBorderRight(BorderStyle.THIN);

        XSSFCellStyle passStyle = getXssfCellStyle(214, 247, 208,workbook, borderStyle);
        XSSFCellStyle failStyle = getXssfCellStyle(252, 197, 197,workbook, borderStyle);

        // Create Header
        Row headerRow = sheet.createRow(0);
        headerRow.setHeightInPoints(40);
        String[] headers = {"ID", "Class_Name", "Method_Name", "Test_Status"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // Add Data
        int rowNum = 1;
        int slNo = 1;
        for (TestFile tfs : testFile) {
            Row row = sheet.createRow(rowNum++);
            row.setHeightInPoints(25);
            Cell cell0 = row.createCell(0);
            cell0.setCellValue(slNo++);
            cell0.setCellStyle(borderStyle);

            Cell cell1 = row.createCell(1);
            cell1.setCellValue(tfs.getClassName());
            cell1.setCellStyle(borderStyle);

            Cell cell2 = row.createCell(2);
            cell2.setCellValue(tfs.getMethodName());
            cell2.setCellStyle(borderStyle);

            Cell cell3 = row.createCell(3);
            cell3.setCellValue(tfs.getTestStatus());
            if ("Passed".equalsIgnoreCase(tfs.getTestStatus())) {
                cell3.setCellStyle(passStyle);
            } else if ("Failed".equalsIgnoreCase(tfs.getTestStatus())) {
                cell3.setCellStyle(failStyle);
            } else {
                cell3.setCellStyle(borderStyle); // Default style
            }
        }

        for (int i = 0; i < 4; i++) {
//            sheet.autoSizeColumn(i);
            sheet.setColumnWidth(i,6000);

        }
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=file.xlsx");

        try {
            workbook.write(response.getOutputStream());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        workbook.close();
    }

    private XSSFCellStyle getXssfCellStyle(int r, int g, int b,XSSFWorkbook workbook, CellStyle borderStyle) {
        XSSFCellStyle passStyle = workbook.createCellStyle();
        passStyle.cloneStyleFrom(borderStyle);
        passStyle.setFillForegroundColor(customizedColumnColor(r,g,b));
        passStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        passStyle.setAlignment(HorizontalAlignment.CENTER);
        passStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        return passStyle;
    }

    public XSSFColor customizedColumnColor(int r, int g, int b){
        Color rgb = new Color(r,g,b);
        return new XSSFColor(rgb,new DefaultIndexedColorMap());
    }

    public List<Map<String, Object>> readExcelSimple(MultipartFile file) throws IOException {
        List<Map<String, Object>> result = new ArrayList<>();

        XSSFWorkbook workbook = new XSSFWorkbook(file.getInputStream());
        XSSFSheet sheet = workbook.getSheetAt(0);
        Row headerRow = sheet.getRow(0);
        int totalCols = headerRow.getLastCellNum();

        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;

            Map<String, Object> rowData = new LinkedHashMap<>();

            for (int j = 0; j < totalCols; j++) {
                String header = headerRow.getCell(j).getStringCellValue().trim();
                String cellValue = row.getCell(j) != null ? row.getCell(j).toString().trim() : "";

                if (header.equalsIgnoreCase("Inputs")) {
                    // Use manual splitting and processing instead of streams
                    cellValue = cellValue.replaceAll("[\\[\\]]","");
                    String[] rawInputs = cellValue.split(",");
                    List<Object> inputs = new ArrayList<>();

                    for (int k = 0; k < rawInputs.length; k++) {
                        String value = rawInputs[k].trim();
                        Object converted = convertToBestType(value);
                        inputs.add(converted);
                    }
                    System.out.println(inputs);
                    rowData.put(header, inputs);
                } else {
                    rowData.put(header, cellValue);
                }
            }
            System.out.println(rowData);
            result.add(rowData);
        }
        System.out.println(result);
        workbook.close();
        return result;
    }

    private Object convertToBestType(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        value = value.trim();

        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {}

        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException ignored) {}

        if (value.length() == 1) {
            return value.charAt(0);
        }
        if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
            return Boolean.parseBoolean(value);
        }

        // Default to String
        return value;
    }

    public Map<String, List<Map<String, Object>>> organizeClassAndMethod(List<Map<String, Object>> excelData) {
        Map<String, List<Map<String, Object>>> result = new HashMap<>();

        for (Map<String, Object> row : excelData) {
            String className = (String) row.get("ClassName");

            if (!result.containsKey(className)) {
                result.put(className, new ArrayList<>());
            }
            result.get(className).add(row);
        }
        return result;
    }


    public String processExcelFile(MultipartFile excelFile) throws IOException {
        StringBuilder testResults = new StringBuilder();

        List<Map<String, Object>> excelData = readExcelSimple(excelFile);
        Map<String, List<Map<String, Object>>> organizedData = organizeClassAndMethod(excelData);

        for (Map.Entry<String, List<Map<String, Object>>> entry : organizedData.entrySet()) {
            String className = entry.getKey();
            List<Map<String, Object>> testCases = entry.getValue();

            for (Map<String, Object> testCase : testCases) {
                String methodName = (String) testCase.get("MethodName");
                List<Object> inputs = (List<Object>) testCase.get("Inputs");
                Object expectedOutput = testCase.get("ExpectedOutput");
                int expectedStatusCode = (Integer) testCase.get("ExpectedStatusCode");

                String result = executeTest(className, methodName, inputs, expectedOutput, expectedStatusCode);
                testResults.append(result).append("\n");
            }
        }
        return testResults.toString();
    }


    public String executeTest(String className, String methodName, List<Object> inputs,
                              Object expectedOutput, int expectedStatusCode) {
        int actualStatusCode = 200; // assume OK for now, since you are not really fetching status code
        Object actualResult = invokeMethod(className, methodName, inputs);
        StringBuilder result = new StringBuilder();

        if (actualStatusCode == expectedStatusCode) {
            result.append("Test passed: ").append(methodName);
        } else {
            result.append("Test failed (StatusCode mismatch): ").append(methodName)
                    .append(" Expected: ").append(expectedStatusCode)
                    .append(" but got: ").append(actualStatusCode);
        }

        if (actualResult != null && actualResult.equals(expectedOutput)) {
            result.append(" | Output matched.");
        } else {
            result.append(" | Output mismatch.");
        }
        return result.toString();
    }


    public Object invokeMethod(String className, String methodName,  List<Object>  params) {
        try {
            // Load the class dynamically
            Class<?> clazz = Class.forName(className);

            // Prepare the parameter types for the method (as Class<?> array)
            Class<?>[] paramTypes = new Class<?>[params.size()];
            for (int i = 0; i < params.size(); i++) {
                paramTypes[i] = params.get(i).getClass();
            }

            // Get the method from the class by name and parameter types
            Method method = clazz.getMethod(methodName, paramTypes);

            // Create an instance of the class
            Object instance = clazz.getDeclaredConstructor().newInstance();

            // Invoke the method and return the result
            return method.invoke(instance, params.toArray());
        } catch (ClassNotFoundException e) {
            System.err.println("Class not found: " + e.getMessage());
        } catch (NoSuchMethodException e) {
            System.err.println("Method not found: " + e.getMessage());
        } catch (IllegalAccessException | InvocationTargetException e) {
            System.err.println("Error invoking method: " + e.getMessage());
        } catch (InstantiationException e) {
            System.err.println("Error creating instance: " + e.getMessage());
        }

        return null;
    }

    public ResponseEntity<String> uploadAndRunTest(MultipartFile javaOrZipFile, MultipartFile testcaseExcelFile) {
        String result = "";
        try {
            String fileName = javaOrZipFile.getOriginalFilename();
            if (fileName == null) {
                return new ResponseEntity<>("Invalid file name.", HttpStatus.BAD_REQUEST);
            }

            // Create a temp directory
            Path tempDir = Files.createTempDirectory("compile_dir");

            if (fileName.endsWith(".zip")) {
                // Handle ZIP file
                File extractedFolder = new File(tempDir.toFile(), fileName.replace(".zip", ""));
                if (!extractedFolder.exists()) {
                    extractedFolder.mkdir();
                }

                extractZipFile(javaOrZipFile, extractedFolder.toPath());

                List<File> javaFiles = getJavaFilesInFolder(extractedFolder);
                if (javaFiles.isEmpty()) {
                    return new ResponseEntity<>("No Java files found in the zip.", HttpStatus.BAD_REQUEST);
                }

                JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
                for (File javaFile : javaFiles) {
                    int compileResult = compiler.run(null, null, null, javaFile.getAbsolutePath());
                    if (compileResult != 0) {
                        throw new RuntimeException("Compilation failed for file: " + javaFile.getName());
                    }
                }

                for (File javaFile : javaFiles) {
                    String originalUploadedFileName = javaFile.getName();
                    extractTestcaseExcelInputAndExecuteTests(javaFile, originalUploadedFileName, testcaseExcelFile);
                }

                result = "All Java files compiled and tests executed successfully!";

            } else if (fileName.endsWith(".java")) {
                // Handle single Java file
                File tempJavaFile = new File(tempDir.toFile(), fileName);
                javaOrZipFile.transferTo(tempJavaFile); // Save the uploaded .java file

                // Directly compile and test
                result = extractTestcaseExcelInputAndExecuteTests(tempJavaFile, fileName, testcaseExcelFile);
            } else {
                return new ResponseEntity<>("Unsupported file type. Please upload a .java file or a .zip folder.", HttpStatus.BAD_REQUEST);
            }

        } catch (Exception e) {
            System.out.println("Exception while processing file: " + e.getMessage());
            return new ResponseEntity<>("Something went wrong while processing file", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return new ResponseEntity<>(result, HttpStatus.OK);
    }



    private String extractTestcaseExcelInputAndExecuteTests(File tempJavaFile, String fileName,MultipartFile testcaseExcelFile ){
        try {
            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            int compiledResult = compiler.run(null,null,null, tempJavaFile.getPath());
            File tempParentDir = tempJavaFile.getParentFile();
            if(compiledResult!=0)
                return "Error occurred while compiling java class file";

            String javaFileName = fileName.replace(".java","");
            String className = "com.example.unit_testing_automation.TestDataFile."+javaFileName;
            URLClassLoader classLoader= URLClassLoader.newInstance(new URL[]{tempParentDir.toURI().toURL()});
            Class<?> clazz = Class.forName(className,true,classLoader);
            Object instance = clazz.getDeclaredConstructor().newInstance();

            List<Map<String,Object>> testCaseList = readExcelSimple(testcaseExcelFile);
            for(Map<String,Object> testcase : testCaseList){
                String methodName = testcase.get("Method Name").toString();
                List<?> inputValuesList = (List<?>) testcase.get("Inputs");
                Object[] inputValues = inputValuesList.toArray();

                String expectedOutput = testcase.get("Expected Output").toString();
                int expectedStatusCode = (int) Double.parseDouble(testcase.get("Expected Status Code").toString());
                for(Method method: clazz.getDeclaredMethods()){
                    if(!method.getName().equals(methodName)) continue;

                    Class<?>[] parameters = method.getParameterTypes();
                    if(inputValues.length != parameters.length) continue;

                    Object[] finalParameterValues = new Object[parameters.length];
                    for(int i=0;i< parameters.length;i++){
                        finalParameterValues[i] = convertToParamType(inputValues[i],parameters[i]);
                    }

                    Object actualResult = method.invoke(instance,finalParameterValues);
                    ResponseEntity<?> actualResponse = (ResponseEntity<?>) actualResult;
                    int actualStatusCode = actualResponse.getStatusCode().value();
                    String actualOutput = Objects.requireNonNull(actualResponse.getBody()).toString();
                    actualOutput = (actualOutput!=null)? actualOutput : "null";
                    String testStatus = (actualOutput.equals(expectedOutput)) && (actualStatusCode == expectedStatusCode)? "Passed" : "Failed";

                    TestFile testFile = new TestFile(javaFileName,methodName,testStatus);
                    repository.save(testFile);
                }
            }
        } catch (Exception e) {
            System.out.println("Exception while running test: "+e.getMessage());
        }
        return "File uploaded and tested successfully";
    }

    private Object convertToParamType(Object inputValue, Class<?> parameter) {
        if(inputValue==null) return null;
        if(parameter.isAssignableFrom(inputValue.getClass())){
            return inputValue;
        }
        String rawValue = inputValue.toString();
        if(parameter == int.class || parameter == Integer.class)
            return Integer.parseInt(rawValue);
        else if(parameter == double.class || parameter == Double.class)
            return Double.parseDouble(rawValue);
        else if(parameter == boolean.class || parameter == Boolean.class)
            return Boolean.parseBoolean(rawValue);
        else if(parameter == long.class || parameter == Long.class)
            return Long.parseLong(rawValue);
        else if(parameter == String.class)
            return rawValue;

        throw new IllegalArgumentException("Unsupported parameter type: "+parameter.getName());
    }
}