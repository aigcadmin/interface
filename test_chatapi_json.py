import os
import json
import requests

IP_ADDRESS = "http://47.251.65.65:80"

class SearchInterface:
    def __init__(self):
        self.url = f"{IP_ADDRESS}/search"
    
    def get_response(self, prompt):
        response = requests.get(self.url+'/'+prompt)
        return response.text
    

class RecomInterface:
    def __init__(self):
        self.url = f"{IP_ADDRESS}/recommend"
    
    def get_response(self, user_id, product_id):
        response = requests.get(self.url+'/'+str(user_id)+'_'+str(product_id))
        return response.text
    
       
class ChatInterface:
    def __init__(self,case_id=1):
        self.url = f"{IP_ADDRESS}/chat"
        self.case_id = case_id
        self.template_path = './buffer/template_dialog.json'
        self.local_path = './buffer/dialog_{}.json'.format(case_id)


    def load_data_from_disk(self):
        # execute the dialog loading process here @backend
        if os.path.exists(self.local_path):
            prompt = json.load(open(self.local_path,'r', encoding='utf-8'))
        else:
            prompt = json.load(open(self.template_path,'r', encoding='utf-8'))
        return prompt


    def post_process(self, prompt, return_data):
        for item in return_data:
            # item = eval(item.decode('utf-8'))
            if item["flag"] == "text":
                print(item["info"], end="")
                # execute the text stream process here @frontend
            elif item["flag"] == "select":
                print(item["info"], end="")
                # execute the select_tag display process here @frontend
            elif item["flag"] == "commodity":
                print('\n--------commodity info---------')
                [print("commidity skuId: {}".format(x["skuId"])) for x in item["info"]]
                # execute the commodity display process here @frontend
            elif item["flag"] == "log":
                prompt.update(item["info"])
                json.dump(prompt,open(self.local_path,'w'))
                # execute the dialog saving process here @backend
            else:
                print("error: unknown flag")
        print()


    def get_response(self, prompt):
        response = requests.post(self.url, json=prompt, stream=True)
        for i, line in enumerate(response.iter_lines()):
            if line:
                decoded_line = json.loads(line.decode("utf-8"))
                yield decoded_line
                

def run_chat_test(app):
    question1 = "油性皮肤用什么化妆品好？,请推荐一款适合油性皮肤的欧莱雅粉底液，价格200块以内，适合室外使用。"
    question2 = "价格太贵了，有没有其他更便宜一些的选项？"
    
    # I.
    prompt = app.load_data_from_disk()
    # step1: user action from frontend
    prompt["current_question"] = question1
    # step2: call the algorithm api
    return_data = app.get_response(prompt)
    # step3: backend process the return data
    app.post_process(prompt, return_data)

    # II.
    prompt = app.load_data_from_disk()
    # step1: user action from frontend
    for qa_pair in prompt["dialog_history_list"][-1]["return_data"]["answer_info"][
        "selection_infos"
    ]:
        if qa_pair["attribute"] != "":
            prompt["selection_result"].append(
                {
                    "attribute": qa_pair["attribute"],
                    "selected": qa_pair["selections"][-1],
                }
            )
    # step2: call the algorithm api
    return_data = app.get_response(prompt)
    # step3: backend process the return data
    app.post_process(prompt, return_data)

    # III.
    prompt = app.load_data_from_disk()
    # step1: user action from frontend
    prompt["current_question"] = question2
    # step2: call the algorithm api
    return_data = app.get_response(prompt)
    # step3: backend process the return data
    app.post_process(prompt, return_data)
    
    return f"case {app.case_id}: Done!"


def run_search_test(app):
    search_res = app.get_response("coat")
    print('\n--------search ... ---------')
    for item in eval(search_res):
        print("commidity skuId: {}".format(item["skuId"]))


def run_recom_test(app):
    # pruduct_id > 0: recommend after the product detail page
    user_id = 1234
    product_id = 10078078061998
    print('\n--------recommend after the product detail page---------')
    recom_res = app.get_response(user_id, product_id)
    for item in eval(recom_res):
        print("commidity skuId: {}".format(item["skuId"]))
    
    # productid = 0: recommend in the main page
    user_id = 1234
    product_id = 0
    print('\n--------recommend in the main page---------')
    recom_res = app.get_response(user_id, product_id)
    for item in eval(recom_res):
        print("commidity skuId: {}".format(item["skuId"]))
    
    # productid = -1: recommend after the shopping cart
    user_id = 1234
    product_id = -1
    print('\n--------recommend after the shopping cart---------')
    recom_res = app.get_response(user_id, product_id)
    for item in eval(recom_res):
        print("commidity skuId: {}".format(item["skuId"]))


if __name__ == "__main__":
    searchApp = SearchInterface()
    run_search_test(searchApp)
    
    recomApp = RecomInterface()
    run_recom_test(recomApp)
    
    chatApp = ChatInterface()
    run_chat_test(chatApp)
    

