const { exec } = require('child_process');

module.exports = {
    handlePrimeDecompose: (req, res) => {
        try {
            const number = req.body.number.replace(/\D/g);
            exec('factor ' + number, (error, stdout, stderr) => {
                console.log(error, stdout, stderr);
                if (error)
                    throw new Error(error);
                if (stderr)
                    throw new Error(stderr);

                const primeNumbers = stdout.match(/\d+/g);
                console.debug('Calculated prime factors for product ' + number + '. Result: ' + JSON.stringify(primeNumbers));

                if (primeNumbers.length === 0)
                    throw new Error('Could not calculate prime numbers');

                return res.status(200).send(String(primeNumbers.pop()));
            });
        } catch (e) {
            console.log('Bad request, parameter is no valid number. ' + e.message);
            return res.status(400).send('Bad request');
        }
    }
};
