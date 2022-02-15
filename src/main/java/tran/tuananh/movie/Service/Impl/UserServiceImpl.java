package tran.tuananh.movie.Service.Impl;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import tran.tuananh.movie.Repository.RoleRepository;
import tran.tuananh.movie.Repository.UserRepository;
import tran.tuananh.movie.Service.RefreshTokenService;
import tran.tuananh.movie.Service.UserService;
import tran.tuananh.movie.Table.DTO.UserDTO;
import tran.tuananh.movie.Table.Model.RefreshToken;
import tran.tuananh.movie.Table.Model.Role;
import tran.tuananh.movie.Table.Model.User;
import tran.tuananh.movie.Table.Model.UserDetail;
import tran.tuananh.movie.Table.Response.*;
import tran.tuananh.movie.Util.JwtUtil;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {

    private final Logger logger = LogManager.getLogger(UserServiceImpl.class);

    @Autowired
    private ModelMapper mapper;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Override
    public Response getAll() {
        return null;
    }

    @Override
    public Response signUp(UserDTO dto) {
        LocalDateTime currentTime = LocalDateTime.now();
        try {
            if (userRepository.existsByUsername(dto.getUsername())) {
                return GenerateResponse.generateErrorResponse("Username " + dto.getUsername() + " is already taken",
                        StatusCode.ERROR);
            }
            if (dto.getEmail() != null && userRepository.existsByEmail(dto.getEmail())) {
                return GenerateResponse.generateErrorResponse("Email " + dto.getEmail() + " is already taken",
                        StatusCode.ERROR);
            }

            Set<Role> roles = new HashSet<>();
            if (dto.getRoles() == null) {
                roles.add(roleRepository.findByName("ROLE_USER"));
            } else {
                for (Role i : dto.getRoles()) {
                    Optional<Role> optionalRole = roleRepository.findById(i.getId());
                    optionalRole.ifPresent(roles::add);
                }
            }

            User user = mapper.map(dto, User.class);
            user.setPassword(passwordEncoder.encode(dto.getPassword()));
            user.setRoles(roles);
            user.setCreatedDate(currentTime);

            userRepository.save(user);
            return GenerateResponse.generateSuccessResponse("SUCCESS SIGN UP", StatusCode.SUCCESS, user);
        } catch (Exception e) {
            logger.error(e);
            return GenerateResponse.generateErrorResponse("FAIL SIGN UP", StatusCode.ERROR);
        }
    }

    @Override
    public Response signIn(UserDTO dto) {
        SignInResponse response = new SignInResponse();

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(dto.getUsername(), dto.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String token = jwtUtil.generateToken(authentication);

        UserDetail userDetail = (UserDetail) authentication.getPrincipal();

        Set<Role> roleList = new LinkedHashSet<>();
        List<String> roleNameList = userDetail.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());
        for (String i : roleNameList) {
            roleList.add(roleRepository.findByName(i));
        }

        User user = mapper.map(userDetail, User.class);
        user.setRoles(roleList);

        RefreshToken refreshToken = refreshTokenService.createRefreshToken(userDetail.getId());

        response.setAccessToken(token);
        response.setRefreshToken(refreshToken.getToken());
        response.setUser(user);
        response.setExpiredAt(jwtUtil.extractExpirationTime(token));
        return GenerateResponse.generateSuccessResponse("SUCCESS SIGN IN", StatusCode.SUCCESS, response);
    }

    @Override
    public Response refreshToken(RefreshToken refreshToken) {
        String requestRefreshToken = refreshToken.getToken();
        RefreshTokenResponse response = new RefreshTokenResponse();
        return refreshTokenService.findByToken(requestRefreshToken).map(refreshTokenService::verifyExpiration)
                .map(RefreshToken::getUser).map(user -> {
                    String token = jwtUtil.generateTokenFromUserName(user.getUsername());
                    response.setAccessToken(token);
                    response.setRefreshToken(requestRefreshToken);
                    return GenerateResponse.generateSuccessResponse("SUCCESS GENERATE REFRESH TOKEN",
                            StatusCode.SUCCESS, response);
                }).orElse(GenerateResponse.generateErrorResponse("Refresh token is not in database", StatusCode.ERROR));
    }

    @Override
    public Response getById(UserDTO dto) {
        return null;
    }

    @Override
    public Response delete(UserDTO dto) {
        return null;
    }
}