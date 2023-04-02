package com.flowershop.serviceImpl;

import com.flowershop.JWT.CustomerUserDetailsService;
import com.flowershop.JWT.JwtFilter;
import com.flowershop.JWT.JwtUtil;
import com.flowershop.POJO.User;
import com.flowershop.constants.FlowerShopConstants;
import com.flowershop.dao.UserDao;
import com.flowershop.service.UserService;
import com.flowershop.utils.EmailUtils;
import com.flowershop.utils.FlowerShopUtils;
import com.flowershop.wrapper.UserWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    UserDao userDao;

    @Autowired
    AuthenticationManager authenticationManager;

    @Autowired
    CustomerUserDetailsService customerUserDetailsService;

    @Autowired
    JwtUtil jwtUtil;

    @Autowired
    JwtFilter jwtFilter;

    @Autowired
    EmailUtils emailUtils;

    @Override
    public ResponseEntity<String> signUp(Map<String, String> requestMap) {
        try {
            log.info("Inside signup");
            if(validateSignUp(requestMap)) {
                User user = userDao.findByEmailId(requestMap.get("email"));
                if(Objects.isNull(user)) {
                    userDao.save(getUserFromMap(requestMap));
                    return FlowerShopUtils.getResponseEntity("Successful sign up!", HttpStatus.OK);
                }
                else {
                    return FlowerShopUtils.getResponseEntity("Email already exists!", HttpStatus.BAD_REQUEST);
                }
            }
            else {
                return FlowerShopUtils.getResponseEntity(FlowerShopConstants.INVALID_DATA, HttpStatus.BAD_REQUEST);
            }
        } catch(Exception ex) {
            ex.printStackTrace();
        }
        return FlowerShopUtils.getResponseEntity(FlowerShopConstants.SOMETHING_WENT_WRONG, HttpStatus.INTERNAL_SERVER_ERROR);

    }

    private boolean validateSignUp(Map<String, String> requestMap) {
        if (requestMap.containsKey("firstName") && requestMap.containsKey("lastName") && requestMap.containsKey("phoneNr")
                && requestMap.containsKey("email") && requestMap.containsKey("password")) {
            return true;
        }
        return false;
    }

    private User getUserFromMap(Map<String, String> requestMap) {
        User user = new User();
        user.setFirstName(requestMap.get("firstName"));
        user.setLastName(requestMap.get("lastName"));
        user.setEmail(requestMap.get("email"));
        user.setPhoneNr(requestMap.get("phoneNr"));
        user.setPassword(requestMap.get("password"));
        user.setRole("user");
        user.setStatus("true");
        return user;
    }

    @Override
    public ResponseEntity<String> login(Map<String, String> requestMap) {
        log.info("Inside login");
        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(requestMap.get("email"), requestMap.get("password"))
            );
            if (auth.isAuthenticated()) {
                if(customerUserDetailsService.getUserDetail().getStatus().equalsIgnoreCase("true")) {
                    return new ResponseEntity<String>("{\"token\":\"" +
                                jwtUtil.generateToken(customerUserDetailsService.getUserDetail().getEmail(),
                                            customerUserDetailsService.getUserDetail().getRole()) + "\"}", HttpStatus.OK);
                }
                else {
                    return new ResponseEntity<String>("{\"message\":\""+ "Wait for admin approval."+"\"}", HttpStatus.BAD_REQUEST);
                }
            }
        } catch (Exception ex) {
            log.error("{}", ex);
        }
        return new ResponseEntity<String>("{\"message\":\""+ "Bad credentials."+"\"}", HttpStatus.BAD_REQUEST);
    }

    @Override
    public ResponseEntity<List<UserWrapper>> getAllUser() {
        try {
            if (jwtFilter.isAdmin()) {
                return new ResponseEntity<>(userDao.getAllUser(), HttpStatus.OK);
            }
            else {
                return new ResponseEntity<>(new ArrayList<>(), HttpStatus.UNAUTHORIZED);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return new ResponseEntity<>(new ArrayList<>(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Override
    public ResponseEntity<String> update(Map<String, String> requestMap) {
        try {

            if(jwtFilter.isAdmin()) {
                Optional<User> optional = userDao.findById(Integer.parseInt(requestMap.get("id")));
                if (!optional.isEmpty()) {
                    userDao.updateStatus(requestMap.get("status"), Integer.parseInt(requestMap.get("id")));
                    sendMailToAllAdmin(requestMap.get("status"), optional.get().getEmail(), userDao.getAllAdmin());
                    return FlowerShopUtils.getResponseEntity("User status updated successfully", HttpStatus.OK);
                }
                else {
                    FlowerShopUtils.getResponseEntity("User id does not exist", HttpStatus.OK);
                }
            } else {
                return FlowerShopUtils.getResponseEntity("Unauthorized access!", HttpStatus.UNAUTHORIZED);
            }
        } catch(Exception ex) {
            ex.printStackTrace();
        }
        return FlowerShopUtils.getResponseEntity(FlowerShopConstants.SOMETHING_WENT_WRONG, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private void sendMailToAllAdmin(String status, String user, List<String> allAdmin) {
        allAdmin.remove(jwtFilter.getCurrentUser());
        if(status!=null && status.equalsIgnoreCase("true")) {
            System.out.println("I sent a massage");
            emailUtils.sendSimpleMessage(jwtFilter.getCurrentUser(), "Account approved", "USER:-" + user + "\n is approved by \nADMIN:-" + jwtFilter.getCurrentUser(), allAdmin);
        }
        else {
            System.out.println("I sent a massage");
            emailUtils.sendSimpleMessage(jwtFilter.getCurrentUser(), "Account disabled", "USER:-" + user + "\n is disabled by \nADMIN:-" + jwtFilter.getCurrentUser(), allAdmin);
        }
    }
}
