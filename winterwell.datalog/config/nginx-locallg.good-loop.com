server {
        listen   80;

        root /home/daniel/winterwell/open-code/winterwell.datalog/web;
        index index.html;

        server_name locallg.good-loop.com;

        location / {
                try_files $uri $uri/ @backend;
                add_header 'Access-Control-Allow-Origin' "$http_origin";
                add_header 'Access-Control-Allow-Credentials' 'true';
        }

        location @backend {
                proxy_pass              http://localhost:8765;
                proxy_set_header        X-Real-IP $remote_addr;
                proxy_set_header        X-Forwarded-For $proxy_add_x_forwarded_for;
                proxy_set_header        Host $http_host;
        }
}

