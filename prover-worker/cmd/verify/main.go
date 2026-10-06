package main

import (
	"deproof.local/prover-worker/internal/circuit"
	"fmt"
	"github.com/consensys/gnark-crypto/ecc"
	"github.com/consensys/gnark/backend/groth16"
	"github.com/consensys/gnark/frontend"
	"io"
	"os"
	"path/filepath"
)

func read(path string, r io.ReaderFrom) error {
	f, e := os.Open(path)
	if e != nil {
		return e
	}
	defer f.Close()
	_, e = r.ReadFrom(io.LimitReader(f, 16*1024*1024))
	return e
}
func run() error {
	if len(os.Args) != 3 {
		return fmt.Errorf("usage: verify PROOF_DIRECTORY EXPECTED_PUBLIC_Y")
	}
	dir := os.Args[1]
	proof := groth16.NewProof(ecc.BN254)
	vk := groth16.NewVerifyingKey(ecc.BN254)
	if e := read(filepath.Join(dir, "proof.bin"), proof); e != nil {
		return e
	}
	if e := read(filepath.Join(dir, "verification-key.bin"), vk); e != nil {
		return e
	}
	public, e := frontend.NewWitness(&circuit.Cubic{Y: os.Args[2]}, ecc.BN254.ScalarField(), frontend.PublicOnly())
	if e != nil {
		return e
	}
	if e = groth16.Verify(proof, vk, public); e != nil {
		return e
	}
	fmt.Println("PASS: local Groth16 relation verified. Verification key trust remains an explicit setup assumption.")
	return nil
}
func main() {
	if e := run(); e != nil {
		fmt.Fprintln(os.Stderr, e)
		os.Exit(1)
	}
}
