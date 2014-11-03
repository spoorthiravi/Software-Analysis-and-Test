function loadEditor(mode, jsonLanguageDef, readOnly) {
    var source = document.getElementById("SourceBox")
    var container = document.getElementById("EditorBox");
    var text = source.value;

    require.config({ baseUrl: "/editor2" });
    require(["vs/editor/editor.main"], function () {
        // require('vs/platform/platform').Registry.getMode(mode).loadAndCreate().then(function () {
        Monaco.Editor.getOrCreateMode(mode).then(function () {
            var options = {
                value: text,
                mode: mode,
                fontIsMonospace: true,
                lineNumbers: true,
                tabSize: 2,
                insertSpaces: true,
                roundedSelection: false,
                scrollBeyondLastLine: false,
                readOnly: readOnly
            };

            if (jsonLanguageDef) options.mode = jsonLanguageDef;
            container.innerHTML = "";
            editor = Monaco.Editor.create(container, options);
        });
    });
}

function selectProgramTextBox(line, column) {
    editor.setPosition({ lineNumber: line, column: column });
}

function updateEditorSource() {
    var sb = document.getElementById('SourceBox');
    var src = editor.getValue();
    sb.value = src;
}

function startDrag() {
    var divider = document.getElementById("divider")
    var programTextBox = document.getElementById('EditorBox')
    var previousOnmouseover = divider.onmouseover
    var previousOnmouseout = divider.onmouseout
    divider.onmouseover = null
    divider.onmouseout = null
    document.body.onmousemove = function (e) {
        if (!e) e = window.event
        var delta = Math.floor(e.clientY - startDragY)
        var height = parseInt(programTextBox.height)
        var newHeight = height + delta;
        programTextBox.style.height = newHeight + 'px';
    }
    document.body.onmouseup = function () {
        document.body.onmousemove = null
        document.body.onmouseup = null
        divider.onmouseover = previousOnmouseover
        divider.onmouseout = previousOnmouseout
    }
}


var decorations = [];
function loadErrors(errors) {
    var modelMirror = editor.getModel();
    var newDecorations = [];
    var range;
    for (var i = 0; i < errors.length; i++) {
        var error = errors[i];
        // line      : The line (relative to 0) at which the lint was found
        // character : The character (relative to 0) at which the lint was found (tabs converted to spaces!!)
        // reason    : The problem
        if (error !== null) {
            var lineNumber = Math.max(Math.min(error.line, modelMirror.getLineCount()), 1);
            var line = modelMirror.getLineContent(lineNumber);
            var character = error.character;
            var startChar = character;
            // for now, only handle spans on one line. (TODO for Peli ;-) )
            var endChar = (error.endLine === lineNumber && error.endColumn != null ? error.endColumn : character);

            if (line[character] === ' ' || line[character] === '\t') {
              // The error character is whitespace, extend to all whitespace around
              while (startChar > 0 && (line[startChar - 1] === ' ' || line[startChar - 1] === '\t')) {
                startChar--;
              }
              while (endChar < line.length - 1 && (line[endChar + 1] === ' ' || line[endChar + 1] === '\t')) {
                endChar++;
              }
            } else if (error.endColumn == null) {
              // This error character is not whitespace, extend to entire line
              var words = line.split(/\b/);
              var startWord;
              var endWord = 0;

              for (var j = 0; j < words.length; j++) {
                var word = words[j].trim();
                if (word.length > 0) {
                  startWord = line.indexOf(word, endWord);
                  endWord = (startWord + word.length) - 1;
                  if (character <= endWord) {
                    startChar = Math.min(character, startWord);
                    endChar = endWord;
                  }
                }
              }
            }
            
            range = {
            };
            range.startLineNumber = lineNumber;
            range.endLineNumber = lineNumber;
            range.startColumn = startChar + 1;
            range.endColumn = endChar + 2;
            if (error.warning) {
                newDecorations.push({ range : range, options : {
                    isOverlay: true,
                    className: "greensquiggly",
                    hoverMessage: error.reason,
                    showInOverviewRuler: "rgba(18,136,18,0.7)"
                }});
            } else {
                newDecorations.push({ range : range, options : {
                    isOverlay: true,
                    className: "redsquiggly",
                    hoverMessage: error.reason,
                    showInOverviewRuler: "rgba(255,18,18,0.7)"
                }});
            }
        }
    }
    decorations = editor.getModel().deltaDecorations(decorations, newDecorations)
}
