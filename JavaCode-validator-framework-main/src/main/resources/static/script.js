document.getElementById('fileType').addEventListener('change', function () {
    const selectedType = this.value;

    // Hide both inputs initially
    document.getElementById('fileInputWrapper').style.display = 'none';
    document.getElementById('zipInputWrapper').style.display = 'none';
    document.getElementById('selectedFile').textContent = 'No file selected';
     document.getElementById('selectedZipFile').textContent = 'No file selected';// Reset

    // Show the appropriate file input based on selection
    if (selectedType === 'java') {
        document.getElementById('fileInputWrapper').style.display = 'block'; // Show Java file input
    } else if (selectedType === 'zip') {
        document.getElementById('zipInputWrapper').style.display = 'block'; // Show ZIP file input
    }
});

// Initial hide for both inputs when the page loads (in case the user reloads the page)
document.getElementById('fileInputWrapper').style.display = 'none';
document.getElementById('zipInputWrapper').style.display = 'none';

// Show selected Excel file name
document.getElementById('excelInput').addEventListener('change', function () {
    const fileName = this.files[0] ? this.files[0].name : 'No file selected';
    document.getElementById('selectedExcelFile').textContent = fileName;
});

// Show selected Java file name
document.getElementById('fileInput').addEventListener('change', function () {
    const fileName = this.files[0] ? this.files[0].name : 'No file selected';
    document.getElementById('selectedFile').textContent = fileName;
});

// Show selected ZIP file name
document.getElementById('zipInput').addEventListener('change', function () {
    const fileName = this.files[0] ? this.files[0].name : 'No file selected';
    document.getElementById('selectedZipFile').textContent = fileName;
});

function uploadFile() {
    const selectedType = document.getElementById('fileType').value;
    let fileInput, fileTypeErrorMessage;

    // Based on selected file type, handle validation
    if (selectedType === 'java') {
        fileInput = document.getElementById('fileInput');
        fileTypeErrorMessage = "Please choose a Java file.";
    } else if (selectedType === 'zip') {
        fileInput = document.getElementById('zipInput');
        fileTypeErrorMessage = "Please choose a ZIP file.";
    }

    const file = fileInput.files[0];
    const excelInput = document.getElementById('excelInput');
    const excelFile = excelInput.files[0];

    // Reset success and error messages
    document.getElementById('uploadSuccess').style.display = 'none';
    document.getElementById('uploadError').style.display = 'none';

    if (!file || !excelFile) {
        document.getElementById('uploadError').textContent = "Please choose both a file and an Excel file.";
        document.getElementById('uploadError').style.display = 'block';
        return;
    }

    if (selectedType === 'java' && !file.name.endsWith('.java')) {
        document.getElementById('uploadError').textContent = "Only Java files are allowed.";
        document.getElementById('uploadError').style.display = 'block';
        return;
    }

    if (selectedType === 'zip' && !file.name.endsWith('.zip')) {
        document.getElementById('uploadError').textContent = "Only ZIP files are allowed.";
        document.getElementById('uploadError').style.display = 'block';
        return;
    }

    // Create FormData
    const formData = new FormData();
    formData.append("javaFile", file); // Append the selected file
    formData.append("excelFile", excelFile);

    // Upload via Fetch
    fetch("http://localhost:8080/unit-test-api/v1/upload-and-runTest", {
        method: "POST",
        body: formData
    })
    .then(response => {
        if (!response.ok) {
            return response.text().then(text => { throw new Error(text); });
        }
        return response.text();
    })
    .then(message => {
        document.getElementById('uploadSuccess').textContent = message;
        document.getElementById('uploadSuccess').style.display = 'block';
        document.getElementById("viewReportsBtn").style.display = "inline-block";
        document.getElementById("downloadReportBtn").style.display = "inline-block";
        document.getElementById('popupMessage').textContent = "Files uploaded and tested successfully.";
        document.getElementById('successPopup').style.display = 'flex';
    })
    .catch(error => {
        document.getElementById('uploadError').textContent = "Upload failed: " + error.message;
        document.getElementById('uploadError').style.display = 'block';
    });
}


// Fetch and display employee test report from backend
function loadEmployees() {
    fetch("http://localhost:8080/unit-test-api/v1/test-reports")
        .then(response => response.json())
        .then(data => {
            const tbody = document.getElementById("reportBody");
            tbody.innerHTML = "";

            // Display each employee's test report in table rows
            data.forEach(emp => {
                let statusClass = '';
                if (emp.testStatus === 'PASSED') {
                    statusClass = 'status-passed';
                } else if (emp.testStatus === 'FAILED') {
                    statusClass = 'status-failed';
                } else if (emp.testStatus === 'SKIPPED') {
                    statusClass = 'status-skipped';
                }

                const row = `
                    <tr>
                        <td>${emp.className}</td>
                        <td>${emp.methodName}</td>
                        <td class="${statusClass}">${emp.testStatus}</td>
                    </tr>
                `;

                tbody.innerHTML += row;
            });

            // Show the table after data is loaded
            document.getElementById("reportTable").style.display = "table";
        })
        .catch(error => {
            document.getElementById('uploadError').textContent = "Error loading test report: " + error.message;
            document.getElementById('uploadError').style.display = 'block';
        });
}

// Export the test report as Excel file
function downloadReport() {
    fetch('http://localhost:8080/unit-test-api/v1/export-data', {
        method: 'GET'
    })
    .then(response => {
        if (!response.ok) {
            throw new Error('Failed to export report');
        }
        return response.blob();
    })
    .then(blob => {
        const url = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.setAttribute('download', 'test_report.xlsx');
        document.body.appendChild(link);
        link.click();
        link.remove();
        URL.revokeObjectURL(url);
    })
    .catch(error => {
        console.error('Error downloading the report:', error);
    });
}

// Function to close popup
function closePopup() {
    document.getElementById('successPopup').style.display = 'none';
}