#!/usr/bin/env python3
import re

# Read the file
with open('src/main/resources/static/ujian.html', 'r', encoding='utf-8') as f:
    content = f.read()

# Replace all remaining template literals with backticks
# Strategy: Find backticks and convert to string concatenation
count = 0

# Pattern 1: alert(`...${var}...`) - convert to alert('...' + var + '...')
# Find lines with alert( backtick
lines = content.split('\n')
new_lines = []
i = 0
while i < len(lines):
    line = lines[i]
    
    # Check if line has alert with backtick
    if 'alert(`' in line and '${' in line:
        # Get the full alert statement (might span multiple lines)
        full_alert = line
        j = i + 1
        while '`);' not in full_alert and j < len(lines):
            full_alert += '\n' + lines[j]
            j += 1
        
        if '`);' in full_alert:
            # Convert: alert(`...${date}...${time}...`) to alert('...' + date + '...' + time + '...')
            # Extract the parts between backticks
            match = re.search(r'alert\(`([^`]*)\`\)', full_alert, re.DOTALL)
            if match:
                template_content = match.group(1)
                # Replace ${variable} with ' + variable + '
                converted = re.sub(r'\$\{([^}]+)\}', r"' + \1 + '", template_content)
                converted = "alert('" + converted + "')"
                
                # Replace only the alert line in line, keep the rest
                if j == i + 1:
                    # Single line
                    new_lines.append(line.replace(match.group(0), converted))
                else:
                    # Multi-line - replace in first line
                    new_lines.append(line.split('alert(`')[0] + converted)
                    # Skip the lines we've already processed
                    i = j
                    continue
                count += 1
            i += 1
        else:
            new_lines.append(line)
            i += 1
    else:
        new_lines.append(line)
        i += 1

new_content = '\n'.join(new_lines)

# Write back to file
with open('src/main/resources/static/ujian.html', 'w', encoding='utf-8') as f:
    f.write(new_content)

print(f"Fixed {count} template literals in ujian.html")
