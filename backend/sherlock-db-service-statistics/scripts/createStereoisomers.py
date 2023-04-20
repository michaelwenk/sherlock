import sys
import rdkit
from rdkit import Chem
from rdkit.Chem.EnumerateStereoisomers import EnumerateStereoisomers, StereoEnumerationOptions

requestID = sys.argv[1]
smiles = sys.argv[2]
fileName = sys.argv[3]

m = Chem.MolFromSmiles(smiles)
opts = StereoEnumerationOptions(unique=True)
isomers = tuple(EnumerateStereoisomers(m, options=opts))

inputSmilesWithStereo = Chem.MolToSmiles(m)
stereoSmiles = [inputSmilesWithStereo]
for smi in sorted(Chem.MolToSmiles(x, isomericSmiles=True) for x in isomers):
	stereoSmiles.append(smi)

stereoSmiles = list(set(stereoSmiles))

writer = Chem.SDWriter(fileName)
for smi in stereoSmiles:
	mTemp = Chem.MolFromSmiles(smi)
	mTemp.SetProp("smiles", smi)
	writer.write(mTemp)

writer.close()