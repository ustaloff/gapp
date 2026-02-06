import requests
import re
import os
from datetime import datetime

# --- Configuration ---
SOURCES = [
    "https://raw.githubusercontent.com/StevenBlack/hosts/master/hosts",
    "https://adaway.org/hosts.txt",
    "https://v.firebog.net/hosts/AdguardDNS.txt",
    "https://v.firebog.net/hosts/Admiral.txt",
    "https://pgl.yoyo.org/adservers/serverlist.php?hostformat=hosts&showintro=0&mimetype=plaintext",
    "https://raw.githubusercontent.com/anudeepND/whitelist/master/domains/optional-list.txt" # Example Whitelist Source (we treat as blocklist in this simple aggregator, but logic below handles whitelisting)
]

# A local whitelist to prevent breaking critical services
# Everything in this list will be REMOVED from the final blocklist
ALLOWLIST = {
    "google.com",
    "android.com",
    "googleapis.com",
    "gstatic.com",
    "gvt1.com", # Google Play updates
    "ggpht.com",
    "play.google.com",
    "firebase.google.com",
    "cloud.google.com",
    "github.com",
    "microsoft.com",
    "apple.com",
    "whatsapp.com",
    "telegram.org",
    "instagram.com",
    "facebook.com", # Core app might functionality might need this
    "analytics.google.com", # Often needed for apps to work (though it tracks)
}

OUTPUT_FILE = "blocklist.txt"

def download_and_parse(url):
    print(f"Downloading from: {url}")
    try:
        response = requests.get(url, timeout=10)
        response.raise_for_status()
        return parse_hosts(response.text)
    except Exception as e:
        print(f"Error downloading {url}: {e}")
        return set()

def parse_hosts(content):
    domains = set()
    for line in content.splitlines():
        line = line.strip()
        # Ignore comments and empty lines
        if not line or line.startswith("#") or line.startswith("!"):
            continue
            
        # Remove 0.0.0.0 or 127.0.0.1 prefix
        parts = line.split()
        if len(parts) >= 2 and (parts[0] == "0.0.0.0" or parts[0] == "127.0.0.1"):
            domain = parts[1]
        elif len(parts) == 1:
            domain = parts[0]
        else:
            continue
            
        # Clean inline comments
        if "#" in domain:
            domain = domain.split("#")[0]
            
        domain = domain.lower().strip()
        
        # Basic validation
        if domain and "." in domain and domain != "localhost":
             domains.add(domain)
             
    return domains

pass # Main Logic
def main():
    all_domains = set()
    
    print("--- AdShield Aggregator Started ---")
    
    # 1. Download & Merge
    for source in SOURCES:
        domains = download_and_parse(source)
        print(f"  -> Found {len(domains)} domains.")
        
        # Deduplication logic check
        before_count = len(all_domains)
        all_domains.update(domains)
        after_count = len(all_domains)
        duplicates_in_batch = len(domains) - (after_count - before_count)
        if duplicates_in_batch > 0:
             print(f"     (Ignored {duplicates_in_batch} duplicates)")
        
    print(f"\nTotal unique raw domains: {len(all_domains)}")
    
    # 2. Apply Whitelist
    print(f"Applying Allowlist ({len(ALLOWLIST)} rules)...")
    final_domains = set()
    for domain in all_domains:
        # Check exact match or subdomain
        # e.g. "ads.google.com" should be allowed if "google.com" is in allowlist? 
        # Strategy: If a domain ENDS WITH an allowlisted domain, we unblock it? 
        # Safe strategy: Only exact matches or explicit subdomains in allowlist?
        # For this script: Exact match + exact check against allowlist items.
        
        is_allowed = False
        for allowed in ALLOWLIST:
             if domain == allowed or domain.endswith("." + allowed):
                 is_allowed = True
                 break
        
        if not is_allowed:
            final_domains.add(domain)
        else:
            # print(f"  Whitelisted: {domain}")
            pass
            
    print(f"Final domain count: {len(final_domains)}")
    
    # 3. Save to File
    base_dir = os.path.dirname(os.path.abspath(__file__))
    output_path = os.path.join(base_dir, OUTPUT_FILE)
    
    with open(output_path, "w", encoding="utf-8") as f:
        # Write Header
        f.write(f"# AdShield Blocklist\n")
        f.write(f"# Generated: {datetime.now().isoformat()}\n")
        f.write(f"# Total Domains: {len(final_domains)}\n\n")
        
        for domain in sorted(final_domains):
            f.write(f"{domain}\n")
            
    print(f"\nSaved to: {output_path}")
    print("--- Done ---")

if __name__ == "__main__":
    main()
