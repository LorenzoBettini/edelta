'use strict';
var net = require('net');
var path = require('path');
var vscode_lc = require('vscode-languageclient');
var spawn = require('child_process').spawn;
function activate(context) {
	var serverInfo = function () {
		// Connect to the language server via a io channel
		var jar = context.asAbsolutePath(path.join('src', 'edelta-uber.jar'));
		var child = spawn('java', ['-jar', jar]);
		child.stdout.on('data', function (chunk) {
			console.log(chunk.toString());
		});
		child.stderr.on('data', function (chunk) {
			console.error(chunk.toString());
		});
		return Promise.resolve(child);
	};
	var clientOptions = {
		documentSelector: ['edelta']
	};
	// Create the language client and start the client.
	var disposable = new vscode_lc.LanguageClient('Edelta', serverInfo, clientOptions).start();
	// Push the disposable to the context's subscriptions so that the 
	// client can be deactivated on extension deactivation
	context.subscriptions.push(disposable);
}
exports.activate = activate;
