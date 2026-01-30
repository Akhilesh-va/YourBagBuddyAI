/**
 * Paste this entire file into your Google Apps Script project (Code.gs).
 * It parses the POST body manually so message, name, emailId, contactNo, rating
 * are always read even when e.parameter is empty for form-encoded POST.
 *
 * Deploy: Deploy > Manage deployments > Edit > New version > Deploy
 */

function doGet(e) {
  return doPost(e);
}

function doPost(e) {
  var spreadsheet = SpreadsheetApp.openByUrl(
    "https://docs.google.com/spreadsheets/d/1T5MUkYZLibgALydNuiB4GUuC3sOkx-ALKkC1ssq8oa4/edit"
  );
  var sheet = spreadsheet.getSheets()[0];

  var data = {};

  // 1) Try e.parameter (query string or form params - sometimes empty for POST body)
  if (e.parameter) {
    data.message = e.parameter.message || "";
    data.name = e.parameter.name || "";
    data.emailId = e.parameter.emailId || "";
    data.contactNo = e.parameter.contactNo || "";
    data.rating = e.parameter.rating || "";
  }

  // 2) If POST body exists, parse it (Apps Script often does NOT put POST body in e.parameter)
  if (e.postData && e.postData.contents) {
    var body = e.postData.contents;
    var type = (e.postData.type || "").toLowerCase();

    if (type.indexOf("application/x-www-form-urlencoded") !== -1) {
      // Parse form body: message=hi&name=John&emailId=...
      var pairs = body.split("&");
      for (var i = 0; i < pairs.length; i++) {
        var parts = pairs[i].split("=");
        if (parts.length >= 2) {
          var key = decodeURIComponent(parts[0].replace(/\+/g, " "));
          var val = decodeURIComponent((parts.slice(1).join("=")).replace(/\+/g, " "));
          data[key] = val;
        }
      }
    } else if (type.indexOf("application/json") !== -1) {
      try {
        var parsed = JSON.parse(body);
        if (parsed.data) parsed = parsed.data;
        data.message = data.message || parsed.Message || parsed.message || "";
        data.name = data.name || parsed.Name || parsed.name || "";
        data.emailId = data.emailId || parsed["Email ID"] || parsed.emailId || parsed.Email || "";
        data.contactNo = data.contactNo || parsed["Contact No."] || parsed.contactNo || parsed.Contact || "";
        data.rating = data.rating || parsed.Rating || parsed.rating || "";
      } catch (err) {}
    }
  }

  var message = data.message || "";
  var name = data.name || "";
  var emailId = data.emailId || "";
  var contactNo = data.contactNo || "";
  var rating = data.rating || "";

  sheet.appendRow([
    new Date(),
    message,
    name,
    emailId,
    contactNo,
    rating
  ]);

  return ContentService
    .createTextOutput(JSON.stringify({ result: "success", message: "Added to Google Sheet" }))
    .setMimeType(ContentService.MimeType.JSON);
}
