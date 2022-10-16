// Check if the required command line tools are available
const whichSync = require('which').sync;
try {
    whichSync('bc'); // will throw if not found
    whichSync('fffactor'); // will throw if not found
} catch {
    console.log('This server requires the command line tools "bc" and "factor".');
    return;
}

const express = require('express');
const app = express();
const port = process.env.PORT || 5221;

const { handlePrimeDecompose } = require('./primes');
const { handleModPow } = require('./modpow');

app.use(express.urlencoded({ extended: true }));

app.post('/primes', handlePrimeDecompose);
app.post('/modpow', handleModPow);

app.listen(port);
console.log('Server started at http://localhost:' + port);
