https://www.bookstack.cn/read/redisson-wiki-zh/spilt.9.14.-%E7%AC%AC%E4%B8%89%E6%96%B9%E6%A1%86%E6%9E%B6%E6%95%B4%E5%90%88.md

# Git

## HTTPS/SSH

HTTPS：[https://github.com/chenzufeng2021/SpringBoot.git]()

SSH：[git@github.com:chenzufeng2021/SpringBoot.git]()

## Git 全局设置

```markdown
git config --global user.name "Chenzufeng"
git config --global user.email "chenzufeng@outlook.com"
```

## 创建 git 仓库

```markdown
echo "# SpringBoot" >> README.md
git init
git add README.md
git commit -m "first commit"
git branch -M main
git remote add origin git@github.com:chenzufeng2021/SpringBoot.git
git push -u origin main
```

## 已有仓库

```markdown
git remote add origin git@github.com:chenzufeng2021/SpringBoot.git
git branch -M main
git push -u origin main
```

# 待归档文档

[Spring Boot 2.X(七)：Spring Cache 使用 - 掘金 (juejin.cn)](https://juejin.cn/post/6844903966615011335)：完善基本的注解信息

[史上超详细的SpringBoot整合Cache使用教程 | Java入门教程_springboot基础教程_springcloud_jvm学习交流网站【Java实验室】 (javawu.com)](https://javawu.com/archives/1731)：涉及SpringCache浅层次的原理

[Spring cache原理详解 - 掘金 (juejin.cn)](https://juejin.cn/post/6959002694539444231#heading-8)

[(42条消息) SpringCache实现原理及核心业务逻辑（一）_不动明王1984的博客-CSDN博客_springcache](https://blog.csdn.net/m0_37962779/article/details/78671468)

[Spring -- Cache原理 - 云+社区 - 腾讯云 (tencent.com)](https://cloud.tencent.com/developer/article/1580633)

[玩转Spring Cache](https://fangshixiang.blog.csdn.net/category_7941357_3.html)：重要

https://blog.csdn.net/qq_43719932/article/details/112651226

AOP：

[(42条消息) Spring AOP实现原理简介_豹变的博客-CSDN博客_springaop实现原理](https://blog.csdn.net/wyl6019/article/details/80136000)

[(42条消息) 细说Spring——AOP详解（AOP概览）_Jivan2233的博客-CSDN博客_aop](https://blog.csdn.net/q982151756/article/details/80513340)

