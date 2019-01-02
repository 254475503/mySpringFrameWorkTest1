package com.auto.sohuyifanshi.framework.servlet;

import com.auto.sohuyifanshi.framework.annotation.Autowired;
import com.auto.sohuyifanshi.framework.annotation.Controller;
import com.auto.sohuyifanshi.framework.annotation.RequestMapping;
import com.auto.sohuyifanshi.framework.annotation.Service;
import com.sun.scenario.effect.impl.sw.sse.SSEBlend_SRC_OUTPeer;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;

public class dispatcherServlet extends HttpServlet {
    private Properties applicationContext;//apllicationContext文件对象
    private List<String> classNames = new ArrayList<String>();//需要扫描的所有类名
    private Map<String,Object> IOCcontainer = new HashMap<String, Object>();//ioc容器
    private Map<String,Method> handlerMapping = new HashMap<String, Method>();
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doPost(req,resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        //6.调用doget或者dopost方法，完成反射调用，并将结果输出到浏览器
        try {
            doDispatcher(req,resp);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            resp.getWriter().write("500 Exception" + Arrays.toString(e.getStackTrace()));
        }
    }

    private void doDispatcher(HttpServletRequest req, HttpServletResponse resp) throws Exception{
        if(this.handlerMapping.isEmpty()){return;}
        String url = req.getRequestURI();
        String contextPath = req.getContextPath();
        url = url.replace(contextPath,"").replaceAll("/+","/");
        if(!this.handlerMapping.containsKey(url))
        {
            resp.getWriter().write("404 Not Found");
            return;
        }
        Method method = handlerMapping.get(url);
        Map<String,String[]>  map= req.getParameterMap();
        Object object = IOCcontainer.get(lowerInitial(method.getDeclaringClass().getSimpleName()));
        method.invoke(object,new Object [] {req,resp,map.get("something")});

        System.out.println("Mapped:"+url+",Method:"+method);
    }

    @Override
    public void init(ServletConfig config) throws ServletException {

        System.out.println(config.getInitParameter("contextConfigLocation"));
        //1.加载配置文件
        doLoadConfig(config.getInitParameter("contextConfigLocation"));//通过config从web.xml文件中读取
        // dispatcherservlet的initparam（contextConfigLocatio）将其为参数传入doLoadConfig，作用是将配置文件的路径传入
        // 加载配置文件的方法

        //2.解析配置文件，扫描所有类上的注解
        doScanner(applicationContext.getProperty("scanPackage"));//读取加载出来的配置文件中的scanpackage属性，
        // 将其作为参数传入doscanner 其中scanpackage里写的是进行注解扫描的包的绝对路径

        //3.创建bean，存到IOC容器中
        doInstance();

        //4.完成自动化的依赖注入autowired
        doAutoWired();

        //5.创建HandlerMapping，将url与相应的method建立映射关系
        initHandlerMapping();

        System.out.println("my SpringFrameWork Init Successfully");


    }
    private void doLoadConfig(String contextConfigLocation) {
        InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(contextConfigLocation);//通过传入的配置文件路径，将配置文件转化为输入流
        try {
            applicationContext.load(inputStream);//将配置文件的输入流转化为一个porperties对象并赋给本servlet申明好的properties变量
            // （在这里是为了方便使用的properties，毕竟是简易的springmvc，配置文件只要配置扫描注解就行。
            // 真正的spring配置文件是xml文件，但也只是配置文件的读取方法不一样而已，这篇教程的springmvc的运行过程是正确的）
            System.out.println("applicationContext.properties loaded");
        } catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            if(null != inputStream) {
                try {
                    inputStream.close();//关闭输入流
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void doScanner(String scanPackage) {
        URL url = this.getClass().getClassLoader().getResource
                ("/"+scanPackage.replace("\\.","/"));//将传入的绝对路径进行转化，转化为一个url
        File classDir = new File(url.getFile());//通过转化完成的url获取一个文件对象
        for (File file : classDir.listFiles()) {//listfiles()是将这个url下面所有的文件和目录全部转化为file对象，
            // 然后将这些对象装配成一个List<File>进行返回

            //递归的进行scan
            if(file.isDirectory()){//如果遍历到的file是一个目录，则继续进行doScanner
                doScanner(scanPackage + "." + file.getName());
            }
            else {//若是一个类的url则将类后面的.class去掉，保留类名，装入本servlet已经初始化的list进行保存
                if(!file.getName().endsWith(".class"))//如扫描出来的结果不是一个class文件就跳过
                    continue;

                String className = (scanPackage + "." +file.getName().replace(".class","").trim());
                classNames.add(className);
            }

        }



    }

    private void doInstance() {
        if(classNames.isEmpty()){return;}
        try {

            for(String string : classNames)
            {
                Class<?> clazz = Class.forName(string);
                if(clazz.isAnnotationPresent(Controller.class))
                {
                    String beanName = lowerInitial(clazz.getSimpleName());

                    IOCcontainer.put(beanName,clazz.newInstance());
                }else if(clazz.isAnnotationPresent(Service.class))
                {
                    Service service = clazz.getAnnotation(Service.class);

                    String beanName = service.value();

                    if("".equals(beanName)) { beanName = lowerInitial(clazz.getSimpleName());}

                    Object object =  clazz.newInstance();

                    IOCcontainer.put(beanName,object);

                    Class<?>[] interfaces = clazz.getInterfaces();

                    for(Class<?> i : interfaces)
                    {
                        String interfaceName = i.getName();
                        if(IOCcontainer.containsKey(interfaceName))
                        {
                            throw  new Exception("The BeanName has Exists");
                        }
                        IOCcontainer.put(interfaceName,clazz);
                    }


                }
                else {
                    continue;
                }
            }


        }catch (Exception e)
        {
            e.printStackTrace();
        }
        finally {
            System.out.println("Beans Creation Finished");
        }

    }

    private void doAutoWired() {
        if(IOCcontainer.isEmpty()){return;}

        for (Map.Entry<String, Object> stringObjectEntry : IOCcontainer.entrySet()) {
            Field[] fields = stringObjectEntry.getValue().getClass().getFields();
            for (Field field : fields) {
                if(!field.isAnnotationPresent(Autowired.class)){continue;}
                Autowired autowired = field.getAnnotation(Autowired.class);
                String beanName = autowired.value();
                if("".equals(beanName))
                {
                    beanName = field.getType().getName();
                }
                field.setAccessible(true);
                try {
                    field.set(stringObjectEntry.getValue(),IOCcontainer.get(beanName));
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                    continue;
                }
            }
            
        }

        System.out.println("Autowiring finished");
    }

    private void initHandlerMapping() {
        if(IOCcontainer.isEmpty()){return;}

        for (Map.Entry<String, Object> entry : IOCcontainer.entrySet()) {
            Class<?> clazz = entry.getValue().getClass();
            String baseUrl = "";
            if(clazz.isAnnotationPresent(Controller.class))
            {
                if(clazz.isAnnotationPresent(RequestMapping.class))
                {
                    RequestMapping requestMapping = clazz.getAnnotation(RequestMapping.class);
                    baseUrl = requestMapping.value();
                }

                Method[] methods = clazz.getDeclaredMethods();
                for(Method method : methods)
                {
                    if(method.isAnnotationPresent(RequestMapping.class))
                    {
                        RequestMapping requestMapping1 = method.getAnnotation(RequestMapping.class);
                        String url = ("/"+baseUrl+"/"+requestMapping1.value()).replace("/+","/");
                        handlerMapping.put(url,method);
                    }
                }
            }

        }

        System.out.println("handlerMapping initing finished");

    }



    public String lowerInitial(String string)
    {
        char[] chars = string.toCharArray();
        chars[0]+=32;
        return String.valueOf(chars);
    }




}
