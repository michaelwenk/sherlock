const fs = require('fs');
const OCL = require("openchemlib");

const smiles = process.argv[2];
const outputFilename = process.argv[3];

const mol = OCL.Molecule.fromSmiles(smiles);
// mol.getIDCodeAndCoordinates();
mol.getDiastereotopicAtomIDs();

fs.writeFile(outputFilename, mol.toMolfileV3(), function(err) {}); 