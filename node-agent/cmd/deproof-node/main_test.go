package main

import "testing"

func TestContainerListenerRequiresExplicitOptIn(t *testing.T) {
	for _, tc := range []struct {
		address string
		opt, ok bool
	}{
		{"127.0.0.1:9843", false, true}, {"[::1]:9843", false, true}, {"0.0.0.0:9843", false, false}, {"0.0.0.0:9843", true, true}, {"10.0.0.2:9843", true, false}, {"node-agent:9843", true, false}, {"invalid", true, false},
	} {
		if (validateListener(tc.address, tc.opt) == nil) != tc.ok {
			t.Errorf("unexpected listener validation %s", tc.address)
		}
	}
}
