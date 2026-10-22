#pragma once
#include <cstdint>
#include <vector>
namespace photo {
// Structural C2PA identification only; the archived JPEG signature is not
// reattached as a credential for the newly encoded HEIC.
void validateC2pa(const std::vector<std::vector<uint8_t>>& packets);
}
