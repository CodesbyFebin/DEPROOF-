// The relation follows gnark's official cubic example at pinned v0.14.0.
// x^3 + x + 5 = y. This statement is unrelated to physical-world contribution.
package circuit

import "github.com/consensys/gnark/frontend"

type Cubic struct {
	X frontend.Variable
	Y frontend.Variable `gnark:",public"`
}

func (c *Cubic) Define(api frontend.API) error {
	api.AssertIsEqual(api.Add(api.Mul(c.X, c.X, c.X), c.X, 5), c.Y)
	return nil
}
