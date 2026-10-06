// Local educational setup only; generated toxic-waste ceremony is not production qualified.
package main

import (
	"crypto/sha256"
	"deproof.local/prover-worker/internal/circuit"
	"encoding/hex"
	"encoding/json"
	"fmt"
	"github.com/consensys/gnark-crypto/ecc"
	"github.com/consensys/gnark/backend/groth16"
	"github.com/consensys/gnark/frontend"
	"github.com/consensys/gnark/frontend/cs/r1cs"
	"io"
	"os"
	"path/filepath"
)

func save(path string, w io.WriterTo) (string, error) {
	f, e := os.OpenFile(path, os.O_CREATE|os.O_EXCL|os.O_WRONLY, 0600)
	if e != nil {
		return "", e
	}
	h := sha256.New()
	_, e = w.WriteTo(io.MultiWriter(f, h))
	if e != nil {
		f.Close()
		return "", e
	}
	if e = f.Sync(); e != nil {
		f.Close()
		return "", e
	}
	e = f.Close()
	return hex.EncodeToString(h.Sum(nil)), e
}
func run() error {
	if len(os.Args) != 2 && len(os.Args) != 3 {
		return fmt.Errorf("usage: prove NEW_OUTPUT_DIRECTORY [PINNED_SETUP_DIRECTORY]")
	}
	dir := os.Args[1]
	if e := os.Mkdir(dir, 0700); e != nil {
		return e
	}
	c := circuit.Cubic{}
	cs, e := frontend.Compile(ecc.BN254.ScalarField(), r1cs.NewBuilder, &c)
	if e != nil {
		return e
	}
	pk := groth16.NewProvingKey(ecc.BN254)
	vk := groth16.NewVerifyingKey(ecc.BN254)
	if len(os.Args) == 3 {
		for name, item := range map[string]io.ReaderFrom{"proving-key.bin": pk, "verification-key.bin": vk} {
			f, err := os.Open(filepath.Join(os.Args[2], name))
			if err != nil {
				return err
			}
			_, err = item.ReadFrom(io.LimitReader(f, 16*1024*1024))
			f.Close()
			if err != nil {
				return err
			}
		}
	} else {
		pk, vk, e = groth16.Setup(cs)
		if e != nil {
			return e
		}
	}
	w, e := frontend.NewWitness(&circuit.Cubic{X: 3, Y: 35}, ecc.BN254.ScalarField())
	if e != nil {
		return e
	}
	proof, e := groth16.Prove(cs, pk, w)
	if e != nil {
		return e
	}
	public, e := w.Public()
	if e != nil {
		return e
	}
	manifest := map[string]string{"schema": "deproof-local-proof-v1", "scheme": "Groth16/BN254", "backend": "gnark-v0.14.0", "relation": "x^3+x+5=y", "publicY": "35", "assurance": "LOCAL_EDUCATIONAL_SETUP; NOT_NETWORK_MEMBERSHIP_OR_REWARD"}
	for name, item := range map[string]io.WriterTo{"proving-key.bin": pk, "circuit.r1cs": cs, "proof.bin": proof, "verification-key.bin": vk, "public-witness.bin": public} {
		digest, e := save(filepath.Join(dir, name), item)
		if e != nil {
			return e
		}
		manifest[name+"Sha256"] = digest
	}
	b, e := json.MarshalIndent(manifest, "", "  ")
	if e != nil {
		return e
	}
	return os.WriteFile(filepath.Join(dir, "manifest.json"), b, 0600)
}
func main() {
	if e := run(); e != nil {
		fmt.Fprintln(os.Stderr, e)
		os.Exit(1)
	}
}
