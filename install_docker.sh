#!/bin/bash

# Exit immediately if a command exits with a non-zero status.
set -e

# Define some colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# --- Helper Functions ---
print_info() {
    echo -e "${BLUE}INFO: $1${NC}"
}

print_success() {
    echo -e "${GREEN}✅ SUCCESS: $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠️ WARNING: $1${NC}"
}

# --- Main Script ---
echo -e "${GREEN}🚀 Starting Docker and Docker Compose Installation Script...${NC}"
echo "This script will prepare your machine to run the FRIZZLY backend."

# Check for root privileges
if [ "$EUID" -ne 0 ]; then
  print_warning "This script requires root privileges."
  echo "Please run with sudo: sudo ./install_docker.sh"
  exit 1
fi

# --- 1. Set up the Docker Repository ---
print_info "Setting up the Docker repository..."
apt-get update
apt-get install -y ca-certificates curl gnupg

print_info "Adding Docker's official GPG key..."
install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | gpg --dearmor -o /etc/apt/keyrings/docker.gpg
chmod a+r /etc/apt/keyrings/docker.gpg

print_info "Adding Docker repository to APT sources..."
echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu \
  $(. /etc/os-release && echo "$VERSION_CODENAME") stable" | \
  tee /etc/apt/sources.list.d/docker.list > /dev/null
print_success "Docker repository setup complete."

# --- 2. Install Docker Engine and Docker Compose ---
print_info "Installing Docker Engine and Docker Compose..."
apt-get update
apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
print_success "Docker and Docker Compose installed."

# --- 3. Post-installation Steps ---
print_info "Adding current user (${SUDO_USER:-$USER}) to the 'docker' group..."
usermod -aG docker ${SUDO_USER:-$USER}
print_success "User added to the docker group."

# --- Final Instructions ---
echo
print_success "🎉 Installation script finished!"
echo
print_warning "A system restart or logout/login is required to apply the user group changes."
echo "After you log back in, please do the following:"
echo
echo -e "1. ${YELLOW}Verify the installation:${NC}"
echo "   docker --version"
echo "   docker compose version"
echo "   docker run hello-world"
echo
echo -e "2. ${YELLOW}Start the backend services:${NC}"
echo "   cd /home/oo33/AndroidStudioProjects/FRIZZLY/backend"
echo "   docker compose up --build -d"
echo
echo -e "3. ${YELLOW}Test the API:${NC}"
echo "   curl http://localhost:5000/check_status"
echo
