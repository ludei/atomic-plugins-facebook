"use strict";

(function(){

    var backgroundTexture, button1Texture, button2Texture, backTexture;
    var fbService, socialAPI;
    var btnLogin, btnLogout, btnShare, btnLoginStatus;
    var container;

    function initProviders() {
        if (window.Cocoon && Cocoon.Social && Cocoon.Social.Facebook) {

            fbService = Cocoon.Social.Facebook; //Pure Facebook API
            fbService.init();
            socialAPI = fbService.getSocialInterface(); //high level abstraction API
            socialAPI.on("loginStatusChanged", function(loggedIn){
                if (btnLogout) 
                    btnLogout.visible = loggedIn;
                if (btnLogin)
                    btnLogin.visible = !loggedIn;
            });
        }
        else {
            alert('Cocoon Facebook Plugin not installed');
        }
    }


    function showControls() {

        //Add buttons
        var btnLogin = createButton("Log In", function(){
            socialAPI.login(function(loggedIn, error){
                if (error) {
                    console.error('Logged Out: ');
                }
            });
        });

        btnLoginStatus = createButton("Login Status", function() {
            fbService.getLoginStatus(function(response) {
                alert("Login status: " + JSON.stringify(response));
                if (response.status === 'connected') {
                    // the user is logged in and has authenticated your
                    // app, and response.authResponse supplies
                    // the user's ID, a valid access token, a signed
                    // request, and the time the access token 
                    // and signed request each expire

                  } else if (response.status === 'not_authorized') {
                    // the user is logged in to Facebook, 
                    // but has not authenticated your app

                  } else {

                  }
            });
        });

        btnLogout = createButton("Log Out", function(){
            socialAPI.logout(function(error){
                console.log('Logged Out: ' + JSON.stringify(error));
            });
        });
        btnLogout.visible = socialAPI && socialAPI.isLoggedIn();
        btnLogin.visible = !btnLogout.visible;

        btnShare = createButton("Share Dialog", function(){
            var message = new Cocoon.Social.Message(
                "Hello from the CocoonJS Launcher App! Are you a HTML5 game developer? Come and check out CocoonJS!",
                "https://cloud.ludei.com/static/images/cocoon_logo.png",
                "http://ludei.com",
                "Ludei & CocoonJS",
                "We love HTML5 games!");

            socialAPI.publishMessageWithDialog(message, function(error) {
                if (error) {
                    console.error("Error publishing message: " + error.message);
                }
            });
        });


        var btnPermissions = createButton("More permissions", function(){
            socialAPI.requestPublishPermissions(function(granted){
               alert('Permissions granted: ' + granted);
            });
        });

        var btnFriends = createButton("Friends using the App", function(){
            socialAPI.requestFriends(function(friends, error){
                if (error) {
                    alert('Error: ' + JSON.stringify(error));
                }
                else {
                    var array = friends || [];
                    console.log("Friends: " + JSON.stringify(array));
                    alert(array.length + " of your friends are using this app. Info printed to console");
                }

            });
        });

        var btnRequestScore = createButton("Request Score", function(){
            socialAPI.requestScore(function(data, error){
                if (error) {
                    alert('Error: ' + JSON.stringify(error));
                }
                else {
                    alert('Score: ' + (data ? data.score : 0) );
                }
            });
        });

        var btnSubmitScore = createButton("Add 10 points to score", function(){
            socialAPI.addScore(10, function(error){
                if (error) {
                    alert('Error: ' + JSON.stringify(error));
                }
                else {
                    alert("Score submitted succesfully");
                }
            });
        });


        var currentY = -200;
        function nextY() {
            var result = currentY;
            currentY += 100;
            return result;
        }
        var logY = nextY();
        btnLogin.position.set(0, logY);
        container.addChild(btnLogin);
        btnLogout.position.set(0, logY);
        container.addChild(btnLogout);
        btnShare.position.set(0, nextY());
        container.addChild(btnShare);
        btnPermissions.position.set(0, nextY());
        container.addChild(btnPermissions);
        btnFriends.position.set(0, nextY());
        container.addChild(btnFriends);
        btnRequestScore.position.set(0, nextY());
        container.addChild(btnRequestScore);
        btnSubmitScore.position.set(0, nextY());
        container.addChild(btnSubmitScore);
        btnLoginStatus.position.set(0, nextY());
        container.addChild(btnLoginStatus);
    }

    function initDemo(){

        initProviders();

        var renderer = PIXI.autoDetectRenderer(window.innerWidth, window.innerHeight);
        document.body.appendChild(renderer.view);

        var W = 800;
        var H = 600;

        //load resources
        backgroundTexture = PIXI.Texture.fromImage('./images/background.jpg');
        button1Texture = PIXI.Texture.fromImage('./images/button1.png');
        button2Texture = PIXI.Texture.fromImage('./images/button2.png');
        backTexture = PIXI.Texture.fromImage('./images/back.png');

        var stage = new PIXI.Container();
        stage.interactive = true;

        //Add background
        var background = new PIXI.Sprite(backgroundTexture);
        background.width = renderer.width;
        background.height = renderer.height;
        stage.addChild(background);

        var scale = Math.min(renderer.width/W, renderer.height/H);
        container = new PIXI.Container();
        container.scale.set(scale, scale);
        container.position.set(renderer.width/2, renderer.height/2);
        stage.addChild(container);

        showControls();
        // start animating
        animate();
        function animate() {
            requestAnimationFrame(animate);
            renderer.render(stage);
        }
    }

    function createButton(text, callback) {

        var button = new PIXI.Sprite(button1Texture);
        button.anchor.set(0.5, 0.5);
        button.interactive = true;
        button.addChild(createText(text));

        button.mousedown = button.touchstart = function(){
            this.texture = button2Texture;
            callback();
        };

        button.mouseup = button.touchend = function(){
            this.texture = button1Texture;
        };

        return button;
    }

    function createBackButton(callback) {
        var button = new PIXI.Sprite(backTexture);
        button.anchor.set(0.5, 0.5);
        button.interactive = true;
        button.addChild(createText("Back"));
        button.mousedown = button.touchstart = function(){
            callback();
        };
        return button;
    }

    function createText(text, size, fill){
        size = size || 25;
        fill = fill || "#ffffff";
        var txt = new PIXI.Text(text, {
            fill: fill,
            font: size + "px Arial"
        });
        txt.anchor.set(0.5, 0.5);

        return txt;
    }

    if (window.cordova) {
        document.addEventListener("deviceready", initDemo);
    }
    else {
        window.onload = initDemo;
    }


})();







