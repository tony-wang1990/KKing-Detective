import urllib.request
import json
import time

url = "https://api.github.com/repos/tony-wang1990/KKing-Detective/actions/runs?per_page=1"
req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0', 'Accept': 'application/vnd.github.v3+json'})
try:
    with urllib.request.urlopen(req) as response:
        data = json.loads(response.read().decode())
        if not data.get('workflow_runs'):
            print("No workflow runs found.")
        else:
            run = data['workflow_runs'][0]
            print(f"Run ID: {run['id']}, Status: {run['status']}, Conclusion: {run['conclusion']}, Head SHA: {run['head_sha']}")
            
            # Fetch jobs for this run
            jobs_url = run['jobs_url']
            jobs_req = urllib.request.Request(jobs_url, headers={'User-Agent': 'Mozilla/5.0'})
            with urllib.request.urlopen(jobs_req) as jobs_response:
                jobs_data = json.loads(jobs_response.read().decode())
                for job in jobs_data.get('jobs', []):
                    print(f"  Job: {job['name']}, Status: {job['status']}, Conclusion: {job['conclusion']}")
                    if job['conclusion'] == 'failure':
                        for step in job['steps']:
                            if step['conclusion'] == 'failure':
                                print(f"    Failed Step: {step['name']}")
except Exception as e:
    print(f"Error: {e}")
