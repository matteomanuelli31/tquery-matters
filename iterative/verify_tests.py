#!/usr/bin/env python3
import json

with open('companies.json') as f:
    data = json.load(f)

projects = []
for company in data['companies']:
    for dept in company['company']['departments']:
        for team in dept['teams']:
            for proj in team['projects']:
                projects.append(proj)

print(f'Total projects: {len(projects)}\n')

# Test 1: in_progress AND Python
test1 = [p['project_id'] for p in projects if p['status'] == 'in_progress' and 'Python' in p['technologies']]
print(f'Test 1 (in_progress AND Python): {sorted(test1)}')

# Test 2: in_progress OR completed
test2 = [p['project_id'] for p in projects if p['status'] in ['in_progress', 'completed']]
print(f'Test 2 (in_progress OR completed): {len(test2)} projects')
print(f'  IDs: {sorted(test2)}')

# Test 3: NOT completed
test3 = [p['project_id'] for p in projects if p['status'] != 'completed']
print(f'Test 3 (NOT completed): {len(test3)} projects')
print(f'  IDs: {sorted(test3)}')

# Test 4: (in_progress AND Python) OR (completed AND Java)
test4a = [p['project_id'] for p in projects if p['status'] == 'in_progress' and 'Python' in p['technologies']]
test4b = [p['project_id'] for p in projects if p['status'] == 'completed' and 'Java' in p['technologies']]
test4 = sorted(set(test4a + test4b))
print(f'Test 4 ((in_progress AND Python) OR (completed AND Java)): {test4}')
print(f'  Part A (in_progress AND Python): {sorted(test4a)}')
print(f'  Part B (completed AND Java): {sorted(test4b)}')

# Status breakdown
statuses = {}
for p in projects:
    statuses[p['status']] = statuses.get(p['status'], 0) + 1
print(f'\nStatus breakdown:')
for status, count in sorted(statuses.items()):
    print(f'  {status}: {count}')
