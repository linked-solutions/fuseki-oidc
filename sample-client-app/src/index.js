import Oidc from 'oidc-client';
const followLinks = () => true;

const signInButton = document.getElementById('signin');
signInButton.addEventListener("click", signin, false);
const loginstatus = document.getElementById('loginstatus');

const authorityField = document.getElementById('authority');
const clientidField = document.getElementById('clientid');

const storage = localStorage;

const settings = {
    //authority: 'https://oidc-wip.factsmission.org/auth/realms/master',
    //client_id: 'frontend',
    redirect_uri: window.location.origin + window.location.pathname,
    post_logout_redirect_uri: window.location.origin + window.location.pathname,
    response_type: 'id_token token',
    scope: 'openid email roles',
    filterProtocolClaims: true,
    loadUserInfo: true
};

function store() {
    try {
        storage.setItem('oidc-settings', JSON.stringify(settings));
    } catch (error) {
        console.log(error)
    }
}

function restore() {
    try {
        if (storage.getItem('oidc-settings')) {
            const storedSettings = JSON.parse(storage.getItem('oidc-settings'));
            authorityField.value = storedSettings.authority || 'https://oidc-wip.factsmission.org/auth/realms/master';
            clientidField.value = storedSettings.client_id || 'frontend';
        }
    } catch (error) {
        console.log(error)
    }
}

function signin() {
    settings.authority = authorityField.value;
    settings.client_id = clientidField.value;
    store()
    const client = new Oidc.OidcClient(settings);
    client.createSigninRequest({
        state: {
            bar: 15
        }
    }).then(function (req) {
        //console.log("signin request", req, req.url);
        if (followLinks()) {
            window.location = req.url;
        }
    }).catch(function (err) {
        console.warn(err);
    });
}

let signinResponse;

function processSigninResponse() {
    settings.authority = authorityField.value;
    settings.client_id = clientidField.value;
    const client = new Oidc.OidcClient(settings);
    client.processSigninResponse().then(function (response) {
        signinResponse = response;
        //console.log("signin response", signinResponse);
        let logoutButton = document.createElement('button');
        logoutButton.innerText = 'Logout';
        logoutButton.addEventListener('click', signout, false);
        let loginstatusText = document.createElement('div');
        loginstatusText.className = 'info';
        loginstatusText.innerText = `Logged in as ${signinResponse.profile.preferred_username}`;
        loginstatus.append(loginstatusText);
        loginstatus.append(logoutButton);
        //signInButton.disabled = true;
        new QueryForm(document.getElementById("queryForm"), "http://localhost:3030/ds/query", `PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
SELECT * WHERE {
    GRAPH ?graph {
        ?sub ?pred ?obj .
    }
} 
LIMIT 10`);
        new QueryForm(document.getElementById("queryForm2"), "http://localhost:3030/ds/update", `PREFIX dc: <http://purl.org/dc/elements/1.1/>
INSERT DATA
{
    GRAPH <http://www.smartswissparticipation.com/graphs/users/${signinResponse.profile.email}> {
        <http://example/book1> dc:title "A new book" ;
                               dc:creator "A.N.Other" .
    }
}`, "POST");
        Array.from(document.getElementsByClassName('sendquery')).forEach(e => e.disabled = false)
    }).catch(function (err) {
        console.warn(err);
    });
}

function signout() {
    settings.authority = authorityField.value;
    settings.client_id = clientidField.value;
    const client = new Oidc.OidcClient(settings);
    client.createSignoutRequest({ id_token_hint: signinResponse && signinResponse.id_token, state: { foo: 5 } }).then(function(req) {
        console.log("signout request", req, "<a href='" + req.url + "'>go signout</a>");
        if (followLinks()) {
            window.location = req.url;
        }
    });
}

function processSignoutResponse() {
    settings.authority = authorityField.value;
    settings.client_id = clientidField.value;
    const client = new Oidc.OidcClient(settings);
    client.processSignoutResponse().then(function(response) {
        signinResponse = null;
        console.log("signout response", response);
    }).catch(function(err) {
        log(err);
    });
}

class QueryForm {
    constructor(element, endpoint, query, method = "GET") {
        let form = `<div class="card">
    <div class="flexrow">
        <label for="endpoint">SPARQL Endpoint:</label>
        <input class="endpoint" type="url" value="${endpoint}">
    </div>
    <div class="flexrow">
        <textarea class="queryfield" rows="10" placeholder="Query">${query || ""}</textarea>
    </div>
    <button disabled class="sendquery">Send Query</button>
    <span class="label">(${method})</span>
    <pre class="error" style="color:red"></pre>
    <pre class="result"></pre>
</div>`
        element.innerHTML = form;
        this.sendQueryButton = element.getElementsByClassName('sendquery')[0];
        this.sendQueryButton.addEventListener("click", () => this.sendQuery(), false);
        this.method = method;
        this.endpointField = element.getElementsByClassName('endpoint')[0];
        this.queryField = element.getElementsByClassName('queryfield')[0];
        this.errorField = element.getElementsByClassName('error')[0];
        this.resultField = element.getElementsByClassName('result')[0];
        if (signinResponse && !signinResponse.error) this.sendQueryButton.disabled = false;
    }


    sendQuery() {
        this.resultField.textContent = "";
        this.errorField.textContent = "";
        let endpoint = this.endpointField.value;
        let query = this.queryField.value;
        let url = this.method === "GET" ? `${endpoint}?query=${encodeURIComponent(query)}` : endpoint;
        let init = {
            method: this.method,
            body: this.method === "POST" ? query : undefined,
            credentials: 'include',
            headers: {
                authorization: `Bearer ${signinResponse.access_token}`
            }
        }
        if (this.method === "POST") init.headers['Content-Type'] = 'application/sparql-update';
        fetch(url, init).then(r => {
            if (r.ok) {
                if (r.status === 204) {
                    this.resultField.textContent = "204: Success";
                } else {
                    r.json().then(j => {
                        console.table(j.results.bindings);
                        this.resultField.textContent = JSON.stringify(j, undefined, "  ")
                    }).catch(e => {
                        console.log("ERROR?: ", e);
                        this.errorField.textContent = e.trimRight();
                    });
                }
            } else {
                r.text().then(t => {
                    console.warn(t);
                    this.errorField.textContent = t.trimRight();
                });
            }
        }).catch(r => {
            console.warn(r);
            this.errorField.textContent = r.trimRight();
        })
    }
}

restore();

if (followLinks()) {
    if (window.location.href.indexOf("#") >= 0) {
        processSigninResponse();
    } else if (window.location.href.indexOf("?") >= 0) {
        processSignoutResponse();
    }
}