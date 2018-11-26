var loader = require("./loader");
var config = loader.config;

loader.load(function(NRS) {
    var data = {
        recipient: NRS.getAccountIdFromPublicKey(config.recipientPublicKey),
        secretPhrase: config.secretPhrase,
        encryptedMessageIsPrunable: "true"
    };
    data = Object.assign(
        data,
        NRS.getMandatoryParams(),
        NRS.encryptMessage(NRS, "message to recipient", config.secretPhrase, config.recipientPublicKey, false)
    );
    NRS.sendRequest("sendMessage", data, function (response) {
        NRS.logConsole(JSON.stringify(response));
    });
});