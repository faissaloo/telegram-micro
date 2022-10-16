//const { modPow } = require('bigint-mod-arith');
const { exec } = require('child_process');

let result = undefined;
let error = undefined;

const respond = res => {
    if (error) {
        return res.status(400).json({error: error});
    }
    if (result === undefined) {
        console.log('Could not calculate modPow in time.')
        return res.status(500).send('Timeout');
    }
    return res.send(String(result));
};

const modPow = (base, exponent, modulus) => {
     exec(`echo "(${base}^${exponent})%${modulus}" | bc`, (error, stdout, stderr) => {
        console.log(error, stdout, stderr);
        if (error)
            throw new Error(error);
        if (stderr)
            throw new Error(stderr);

        result = Number(stdout.match(/\d+/g));
    });
};

module.exports = {
    handleModPow: (req, res) => {
        setTimeout(() => respond(res), 2000); // to avoid timing attacks, try to make this fixed time
        const [ base, exponent, modulus ] = ['base', 'exponent', 'modulus'].map(k => req.query[k]);
        console.debug(`Calculating modpow with base ${base} exponent ${exponent} modulus ${modulus}...`);

        const isValid = n => n && n.match(/^\d+$/).length === 1;
        if (!isValid(base) || !isValid(exponent) || !isValid(modulus)) {
            console.log('Bad request, base, exponent or modulus is bad value');
            error = 'Bad request';
        }
        try {
            result = modPow(base, exponent, modulus);
            console.debug('...result: ' + result);
        } catch (e) {
            error = e.message;
        }
    }
};
