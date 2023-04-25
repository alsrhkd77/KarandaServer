FROM python:3.10.7

WORKDIR /code

COPY ./requirements.txt /code/requirements.txt

COPY ./require /code/require

#RUN pip install --no-cache-dir --upgrade ./require/jws-0.1.3/

RUN pip install --upgrade pip

RUN pip install --no-cache-dir --upgrade -r /code/requirements.txt

COPY ./app /code/app

CMD ["export", "env=deploy"]
CMD ["export", "web_front_url=https://www.karanda.kr"]

CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8080"]