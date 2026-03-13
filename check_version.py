import urllib.request
import json

url = "https://search.maven.org/solrsearch/select?q=g:com.oracle.oci.sdk+AND+a:oci-java-sdk-core&rows=5&wt=json"
req = urllib.request.urlopen(url)
data = json.loads(req.read())
print([doc['v'] for doc in data['response']['docs']])
