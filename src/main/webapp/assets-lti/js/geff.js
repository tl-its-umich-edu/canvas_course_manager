(function() {
  var targetColumns = ['SIS Login ID', 'Final Grade'];
  var outputFilename;
  var $fileInfo = $('#upload-file-info'),
    $fileInput = $("#csv-file");
  var dropZone = document.getElementById('drop-zone');

  function handleFileSelect(evt) {
    var file;
    if (evt.type === 'drop') {
      file = evt.dataTransfer.files[0];
      evt.stopPropagation();
      evt.preventDefault();
    } else {
      file = evt.target.files[0];
    }

    Papa.parse(file, {
      header: true,
      dynamicTyping: true,
      complete: parseCompleteHandler
    });

    var splitName = file.name.split('.'),
      filenameIndex = splitName.length >= 2 ? splitName.length - 2 : 0;

    splitName[filenameIndex] = splitName[filenameIndex] + '-geff';
    outputFilename = splitName.join('.');

    $fileInfo
    .empty()
    .append('<div><b>Input: </b>' + file.name + '</div>' );
  }

  function parseCompleteHandler(results) {
    //reset errors and hide message panels
    errorCount = [];
    $('#noGradingSchemaMessage').fadeOut('fast');
    $('#currentFinalMismatch').fadeOut('fast');

    // check to see if Final Grade is available
    if(results.data[0].hasOwnProperty('Final Grade')){
      var targetData = results.data.slice(1); //ignore first row
      // see if all students have the same Current Grade and Final Grade
      validateData(targetData, errorCount);
      // if there are stduents where Current Grade !== Final Grade
      // show a message with numner and names of students affected
      if(errorCount.length !==0){
        $('#studentListCount').text(errorCount.length);
        $('#studentList').text(errorCount.join(', '));
        $('#currentFinalMismatch').fadeIn('slow');
      } else {
        // there were no Current Grade and Final Grade mismatches, proceed
        formattedData = formatData(targetData, targetColumns);
        downloadCSVFile(formattedData);
      }
    } else {
      //show message about the lack of grading scheme and Final Grade
      $('#noGradingSchemaMessage').fadeIn('slow');
    }
  }

  function validateData(targetData, errorCount ) {
    _.each(targetData, function(row){
      if(row['Final Grade'] !== row['Current Grade']){
        errorCount.push(row.Student + ' (' + row['SIS Login ID'] + ')');
      }
    });
    //return(errorCount);
  }

  function formatData(data, targetCols) {
    return data.map(function(row) {
      return targetCols.map(function(col) {
        return row[col];
      });
    });
  }

  function downloadCSVFile(data) {
    // credit: http://stackoverflow.com/questions/14964035/how-to-export-javascript-array-info-to-csv-on-client-side
    var csvContent = "data:text/csv;charset=utf-8,";
    data.forEach(function(infoArray, index) {
      dataString = infoArray.join(",");
      csvContent += index < data.length ? dataString + "\n" : dataString;
    });
    var encodedUri = encodeURI(csvContent);
    var link = document.createElement("a");
    link.setAttribute("href", encodedUri);
    link.setAttribute("download", outputFilename);
    document.body.appendChild(link); // Required for FF
    // delay to give impression of processing file
    setTimeout(function() {
      link.click(); // This will download the data file
      var $outputMessage = $('<div></div>')
        .hide()
        .append('<div><b>Output: </b>' + outputFilename + '</div>')
        .append('<div class="mpathways" style="padding-top: 10px;font-size: 40px;"><a href="https://csprod.dsc.umich.edu/services/faculty/" target="_blank">Upload to Faculty Center <i class="glyphicon glyphicon-upload"></i></a></div>')
        .appendTo($fileInfo)
        .fadeIn();
    }, 800);
  }

  function handleDragOver(evt) {
    evt.stopPropagation();
    $('#drop-zone').css('border-color','#000');
    evt.preventDefault();
    evt.dataTransfer.dropEffect = 'copy'; // Explicitly show this is a copy.
  }

  function handleDragLeave(evt) {
      $('#drop-zone').css('border-color','#aaa');
  }

  $(document).ready(function() {
    // credit: https://www.html5rocks.com/en/tutorials/file/dndfiles/
    dropZone.addEventListener('dragover', handleDragOver, false);
    dropZone.addEventListener('dragleave', handleDragLeave, false);
    dropZone.addEventListener('drop', handleFileSelect, false);
    $fileInput.on('change', handleFileSelect);
  });
})();
