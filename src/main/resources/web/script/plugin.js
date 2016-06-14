// configure CKEditor image upload and browsing
dm4c.add_plugin('de.deepamehta.images', function () {
    // config lines are the same / work with "image2" plugin as well
    CKEDITOR.config.filebrowserImageBrowseUrl = '/de.deepamehta.images/browse.html'
    CKEDITOR.config.filebrowserImageUploadUrl = '/images/upload/ckeditor'

    var selectedMaxSize = undefined
    var selectedMode = undefined

    function openResizeDialog() {
        var resizeModeMenu = dm4c.ui.menu(function(result) {
            selectedMode = result.value
        })
        resizeModeMenu.add_item({"label": "Auto", "value": "auto"})
        resizeModeMenu.add_item({"label": "Width", "value": "width"})
        resizeModeMenu.add_item({"label": "Height", "value": "height"})

        var sizeMenu = dm4c.ui.menu(function(result) {
            selectedMaxSize = result.value
        })
        sizeMenu.add_item({"label": "90px", "value": 90})
        sizeMenu.add_item({"label": "160px", "value": 160})
        sizeMenu.add_item({"label": "300px", "value": 300})
        sizeMenu.add_item({"label": "420px", "value": 420})
        sizeMenu.add_item({"label": "540px", "value": 540})
        sizeMenu.add_item({"label": "600px", "value": 600})
        sizeMenu.add_item({"label": "720px", "value": 720})
        sizeMenu.add_item({"label": "900px", "value": 900})
        sizeMenu.add_item({"label": "1000px", "value": 1000})
        sizeMenu.add_item({"label": "1200px", "value": 1200})
        sizeMenu.add_item({"label": "1400px", "value": 1400})
        sizeMenu.add_item({"label": "1600px", "value": 1600})
        sizeMenu.add_item({"label": "2000px", "value": 2000})
        //
        var labelSize = $('<span>').attr("class", "field-label").html("Max. Size<br/>")
        var fitToMode = $('<span>').attr("class", "field-label").html("Fit to<br/>")
        var dialogBody = $('<div>').append(labelSize).append(sizeMenu.dom).append('<br/>')
                .append(fitToMode).append(resizeModeMenu.dom)
        var resizeSettingsDialog = dm4c.ui.dialog({
            "id": 'resize-options',
            "width": 175,
            "title": 'Resize Options',
            "content": dialogBody,
            "button_label": "Do Resize",
            "button_handler": function(e) {
                var imageTopic = dm4c.restc.request('GET', '/images/resize/' + dm4c.selected_object.id + "/" + selectedMaxSize + "/" + selectedMode)
                dm4c.show_topic(new Topic(imageTopic), "show", undefined, true)
                resizeSettingsDialog.close()
            },
            "auto_close": false
        })
        console.log("Resize Dialog", resizeSettingsDialog)
        if (selectedMaxSize) {
            sizeMenu.select(selectedMaxSize)
        } else {
            sizeMenu.select(300)
        }
        if (selectedMode) {
            resizeModeMenu.select(selectedMode)
        } else {
            resizeModeMenu.select("auto")
        }
    }

    dm4c.add_listener('topic_commands', function (topic) {
        // Note: create permission now managed by core
        var commands = []
        if (topic.type_uri === 'dm4.files.file' && dm4c.restc.get_username()) {
            if (topic.childs["dm4.files.media_type"].value === "image/jpeg" ||
                topic.childs["dm4.files.media_type"].value === "image/png")
            commands.push({is_separator: true, context: 'context-menu'})
            commands.push({
                label: 'Resize..',
                handler: openResizeDialog,
                context: ['context-menu', 'detail-panel-show']
            })
        }
        return commands
    })
})
