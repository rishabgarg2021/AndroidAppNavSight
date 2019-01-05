/*
 * Author: Uvin Abeysinghe
 * Student Id : 789931
 * University of Melbourne
 */

'use strict'

const functions = require('firebase-functions');
const admin = require('firebase-admin');
admin.initializeApp(functions.config().firebase);

//help request notifications.
exports.sendNotification = functions.database.ref('/help_req/{UserId}/{notification_id}/').onWrite((change, context) =>
{
    const UserId = context.params.UserId;
    const notification = context.params.notification_id;

    console.log('The user Id is : ', UserId);

    if(!change.after.exists())
    {
        return console.log('A Notification has been deleted from the database : ', notification_id);
    }

    if (!change.after.exists())
    {
        return console.log('A notification has been deleted from the database:', notification);
    }

    const deviceToken = admin.database().ref(`/tokens/${UserId}/device_token`).once('value');

    return deviceToken.then(result =>
    {
        const token_id = result.val();
        const payload = {
            data : {
                title : `Help`,
                body : `${notification}`,
            }
        };
        console.log('Send token is ',token_id);

        return admin.messaging().sendToDevice(token_id, payload).then(response => {

            return console.log('This was the notification Feature');
        });
    });
});

//chat messages notification
exports.sendMessNoti = functions.database.ref('/messages/{whoWhom}/{notification_id}/').onWrite((change, context) =>
{
    const UserId = context.params.whoWhom;
    const notification = context.params.notification_id;

    console.log('The user Id is : ', UserId);
    console.log('The notification is : ', notification);

    if(!change.after.exists())
    {
        return console.log('A Notification has been deleted from the database : ', notification_id);
    }

    if (!change.after.exists())
    {
        return console.log('A notification has been deleted from the database:', notification);
    }


    const to = admin.database().ref(`/messages/${UserId}/${notification}/user`).once('value');

    return to.then(result=>{
        const to_id = result.val();
        console.log('The new user Id is : ', to_id);




        const deviceToken = admin.database().ref(`/tokens/${to_id}/device_token`).once('value');


        return deviceToken.then(result =>
        {
            const token_id = result.val();

            const payload = {
                data : {
                    title : 'Message',
                    notiID : `${UserId}`,
                    pushID : `${notification}`

                }
            };
            console.log('Send token is ',token_id);

            return admin.messaging().sendToDevice(token_id, payload).then(response => {

                return console.log('This was the notification Feature');
            });
        });

    });


});
